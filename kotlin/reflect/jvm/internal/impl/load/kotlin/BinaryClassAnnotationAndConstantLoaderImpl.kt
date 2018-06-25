/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.reflect.jvm.internal.impl.load.kotlin

import kotlin.reflect.jvm.internal.impl.descriptors.*
import kotlin.reflect.jvm.internal.impl.descriptors.annotations.AnnotationDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.annotations.AnnotationDescriptorImpl
import kotlin.reflect.jvm.internal.impl.descriptors.annotations.AnnotationUseSiteTarget
import kotlin.reflect.jvm.internal.impl.descriptors.annotations.AnnotationWithTarget
import kotlin.reflect.jvm.internal.impl.load.java.components.DescriptorResolverUtils
import kotlin.reflect.jvm.internal.impl.load.kotlin.KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor
import kotlin.reflect.jvm.internal.impl.name.ClassId
import kotlin.reflect.jvm.internal.impl.name.Name
import kotlin.reflect.jvm.internal.impl.resolve.constants.*
import kotlin.reflect.jvm.internal.impl.serialization.ProtoBuf
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.AnnotationDeserializer
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.NameResolver
import kotlin.reflect.jvm.internal.impl.storage.StorageManager
import kotlin.reflect.jvm.internal.impl.utils.compact
import java.util.*

class BinaryClassAnnotationAndConstantLoaderImpl(
        private val module: ModuleDescriptor,
        private val notFoundClasses: NotFoundClasses,
        storageManager: StorageManager,
        kotlinClassFinder: KotlinClassFinder
) : AbstractBinaryClassAnnotationAndConstantLoader<AnnotationDescriptor, ConstantValue<*>, AnnotationWithTarget>(
        storageManager, kotlinClassFinder
) {
    private val annotationDeserializer = AnnotationDeserializer(module, notFoundClasses)

    override fun loadTypeAnnotation(proto: ProtoBuf.Annotation, nameResolver: NameResolver): AnnotationDescriptor =
            annotationDeserializer.deserializeAnnotation(proto, nameResolver)

    override fun loadConstant(desc: String, initializer: Any): ConstantValue<*>? {
        val normalizedValue: Any = if (desc in "ZBCS") {
            val intValue = initializer as Int
            when (desc) {
                "Z" -> intValue != 0
                "B" -> intValue.toByte()
                "C" -> intValue.toChar()
                "S" -> intValue.toShort()
                else -> throw AssertionError(desc)
            }
        }
        else {
            initializer
        }

        return ConstantValueFactory.createConstantValue(normalizedValue)
    }

    override fun loadPropertyAnnotations(
            propertyAnnotations: List<AnnotationDescriptor>,
            fieldAnnotations: List<AnnotationDescriptor>,
            fieldUseSiteTarget: AnnotationUseSiteTarget
    ): List<AnnotationWithTarget> {
        return propertyAnnotations.map { AnnotationWithTarget(it, null) } +
               fieldAnnotations.map { AnnotationWithTarget(it, fieldUseSiteTarget) }
    }

    override fun transformAnnotations(annotations: List<AnnotationDescriptor>): List<AnnotationWithTarget> {
        return annotations.map { AnnotationWithTarget(it, null) }
    }

    override fun loadAnnotation(
            annotationClassId: ClassId,
            source: SourceElement,
            result: MutableList<AnnotationDescriptor>
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
        val annotationClass = resolveClass(annotationClassId)

        return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor {
            private val arguments = HashMap<Name, ConstantValue<*>>()

            override fun visit(name: Name?, value: Any?) {
                if (name != null) {
                    arguments[name] = createConstant(name, value)
                }
            }

            override fun visitEnum(name: Name, enumClassId: ClassId, enumEntryName: Name) {
                arguments[name] = EnumValue(enumClassId, enumEntryName)
            }

            override fun visitArray(name: Name): AnnotationArrayArgumentVisitor? {
                return object : KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor {
                    private val elements = ArrayList<ConstantValue<*>>()

                    override fun visit(value: Any?) {
                        elements.add(createConstant(name, value))
                    }

                    override fun visitEnum(enumClassId: ClassId, enumEntryName: Name) {
                        elements.add(EnumValue(enumClassId, enumEntryName))
                    }

                    override fun visitEnd() {
                        val parameter = DescriptorResolverUtils.getAnnotationParameterByName(name, annotationClass)
                        if (parameter != null) {
                            arguments[name] = ConstantValueFactory.createArrayValue(elements.compact(), parameter.type)
                        }
                    }
                }
            }

            override fun visitAnnotation(name: Name, classId: ClassId): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                val list = ArrayList<AnnotationDescriptor>()
                val visitor = loadAnnotation(classId, SourceElement.NO_SOURCE, list)!!
                return object: KotlinJvmBinaryClass.AnnotationArgumentVisitor by visitor {
                    override fun visitEnd() {
                        visitor.visitEnd()
                        arguments[name] = AnnotationValue(list.single())
                    }
                }
            }

            override fun visitEnd() {
                result.add(AnnotationDescriptorImpl(annotationClass.defaultType, arguments, source))
            }

            private fun createConstant(name: Name?, value: Any?): ConstantValue<*> {
                return ConstantValueFactory.createConstantValue(value)
                       ?: ErrorValue.create("Unsupported annotation argument: $name")
            }
        }
    }

    private fun resolveClass(classId: ClassId): ClassDescriptor {
        return module.findNonGenericClassAcrossDependencies(classId, notFoundClasses)
    }
}
