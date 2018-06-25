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

package kotlin.reflect.jvm.internal.impl.load.java.descriptors

import kotlin.reflect.jvm.internal.impl.descriptors.CallableDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.ClassDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.ValueParameterDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.annotations.AnnotationDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.impl.ValueParameterDescriptorImpl
import kotlin.reflect.jvm.internal.impl.load.java.JvmAnnotationNames
import kotlin.reflect.jvm.internal.impl.load.java.lazy.descriptors.LazyJavaStaticClassScope
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmPackagePartSource
import kotlin.reflect.jvm.internal.impl.resolve.constants.StringValue
import kotlin.reflect.jvm.internal.impl.resolve.descriptorUtil.firstArgument
import kotlin.reflect.jvm.internal.impl.resolve.descriptorUtil.getSuperClassNotAny
import kotlin.reflect.jvm.internal.impl.resolve.descriptorUtil.module
import kotlin.reflect.jvm.internal.impl.resolve.jvm.JvmClassName
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.descriptors.DeserializedMemberDescriptor
import kotlin.reflect.jvm.internal.impl.types.KotlinType
import kotlin.reflect.jvm.internal.impl.utils.addToStdlib.safeAs

class ValueParameterData(val type: KotlinType, val hasDefaultValue: Boolean)

fun copyValueParameters(
        newValueParametersTypes: Collection<ValueParameterData>,
        oldValueParameters: Collection<ValueParameterDescriptor>,
        newOwner: CallableDescriptor
): List<ValueParameterDescriptor> {
    assert(newValueParametersTypes.size == oldValueParameters.size) {
        "Different value parameters sizes: Enhanced = ${newValueParametersTypes.size}, Old = ${oldValueParameters.size}"
    }

    return newValueParametersTypes.zip(oldValueParameters).map { (newParameter, oldParameter) ->
        ValueParameterDescriptorImpl(
                newOwner,
                null,
                oldParameter.index,
                oldParameter.annotations,
                oldParameter.name,
                newParameter.type,
                newParameter.hasDefaultValue,
                oldParameter.isCrossinline,
                oldParameter.isNoinline,
                if (oldParameter.varargElementType != null) newOwner.module.builtIns.getArrayElementType(newParameter.type) else null,
                oldParameter.source
        )
    }
}

fun ClassDescriptor.getParentJavaStaticClassScope(): LazyJavaStaticClassScope? {
    val superClassDescriptor = getSuperClassNotAny() ?: return null

    val staticScope = superClassDescriptor.staticScope

    if (staticScope !is LazyJavaStaticClassScope) return superClassDescriptor.getParentJavaStaticClassScope()

    return staticScope
}

fun DeserializedMemberDescriptor.getImplClassNameForDeserialized(): JvmClassName? =
        (containerSource as? JvmPackagePartSource)?.className

fun DeserializedMemberDescriptor.isFromJvmPackagePart(): Boolean =
        containerSource is JvmPackagePartSource

fun ValueParameterDescriptor.getParameterNameAnnotation(): AnnotationDescriptor? {
    val annotation = annotations.findAnnotation(JvmAnnotationNames.PARAMETER_NAME_FQ_NAME) ?: return null
    if (annotation.firstArgument()?.safeAs<StringValue>()?.value?.isEmpty() != false) {
        return null
    }

    return annotation
}

sealed class AnnotationDefaultValue
class StringDefaultValue(val value: String) : AnnotationDefaultValue()
object NullDefaultValue : AnnotationDefaultValue()

fun ValueParameterDescriptor.getDefaultValueFromAnnotation(): AnnotationDefaultValue? {
    annotations.findAnnotation(JvmAnnotationNames.DEFAULT_VALUE_FQ_NAME)
            ?.firstArgument()
            ?.safeAs<StringValue>()?.value
            ?.let { return StringDefaultValue(it) }

    if (annotations.hasAnnotation(JvmAnnotationNames.DEFAULT_NULL_FQ_NAME)) {
        return NullDefaultValue
    }

    return null
}
