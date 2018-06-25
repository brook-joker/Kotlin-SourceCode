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

package kotlin.reflect.jvm.internal.impl.load.java.lazy.descriptors

import kotlin.reflect.jvm.internal.impl.descriptors.ClassDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.SourceElement
import kotlin.reflect.jvm.internal.impl.descriptors.annotations.Annotations
import kotlin.reflect.jvm.internal.impl.descriptors.impl.PackageFragmentDescriptorImpl
import kotlin.reflect.jvm.internal.impl.load.java.lazy.LazyJavaResolverContext
import kotlin.reflect.jvm.internal.impl.load.java.lazy.childForClassOrPackage
import kotlin.reflect.jvm.internal.impl.load.java.lazy.resolveAnnotations
import kotlin.reflect.jvm.internal.impl.load.java.structure.JavaClass
import kotlin.reflect.jvm.internal.impl.load.java.structure.JavaPackage
import kotlin.reflect.jvm.internal.impl.load.kotlin.KotlinJvmBinaryPackageSourceElement
import kotlin.reflect.jvm.internal.impl.load.kotlin.header.KotlinClassHeader
import kotlin.reflect.jvm.internal.impl.name.ClassId
import kotlin.reflect.jvm.internal.impl.name.FqName
import kotlin.reflect.jvm.internal.impl.resolve.jvm.JvmClassName
import kotlin.reflect.jvm.internal.impl.storage.getValue

class LazyJavaPackageFragment(
        outerContext: LazyJavaResolverContext,
        private val jPackage: JavaPackage
) : PackageFragmentDescriptorImpl(outerContext.module, jPackage.fqName) {
    private val c = outerContext.childForClassOrPackage(this)

    internal val binaryClasses by c.storageManager.createLazyValue {
        c.components.packageMapper.findPackageParts(fqName.asString()).mapNotNull { partName ->
            val classId = ClassId.topLevel(JvmClassName.byInternalName(partName).fqNameForTopLevelClassMaybeWithDollars)
            c.components.kotlinClassFinder.findKotlinClass(classId)?.let { partName to it }
        }.toMap()
    }

    private val scope = JvmPackageScope(c, jPackage, this)

    private val subPackages = c.storageManager.createRecursionTolerantLazyValue(
            { jPackage.subPackages.map(JavaPackage::fqName) },
            // This breaks infinite recursion between loading Java descriptors and building light classes
            onRecursiveCall = listOf()
    )

    override val annotations =
            // Do not resolve package annotations if JSR-305 is disabled
            if (c.components.annotationTypeQualifierResolver.disabled) Annotations.EMPTY
            else c.resolveAnnotations(jPackage)

    internal fun getSubPackageFqNames(): List<FqName> = subPackages()

    internal fun findClassifierByJavaClass(jClass: JavaClass): ClassDescriptor? = scope.javaScope.findClassifierByJavaClass(jClass)

    private val partToFacade by c.storageManager.createLazyValue {
        val result = hashMapOf<JvmClassName, JvmClassName>()
        kotlinClasses@for ((partInternalName, kotlinClass) in binaryClasses) {
            val partName = JvmClassName.byInternalName(partInternalName)
            val header = kotlinClass.classHeader
            when (header.kind) {
                KotlinClassHeader.Kind.MULTIFILE_CLASS_PART -> {
                    result[partName] = JvmClassName.byInternalName(header.multifileClassName ?: continue@kotlinClasses)
                }
                KotlinClassHeader.Kind.FILE_FACADE -> {
                    result[partName] = partName
                }
                else -> {}
            }
        }
        result
    }

    fun getFacadeNameForPartName(partName: JvmClassName): JvmClassName? = partToFacade[partName]

    override fun getMemberScope() = scope

    override fun toString() = "Lazy Java package fragment: $fqName"

    override fun getSource(): SourceElement = KotlinJvmBinaryPackageSourceElement(this)
}
