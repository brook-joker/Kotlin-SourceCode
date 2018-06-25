/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package kotlin.reflect.jvm.internal.impl.types

import kotlin.reflect.jvm.internal.impl.builtins.KotlinBuiltIns
import kotlin.reflect.jvm.internal.impl.descriptors.annotations.Annotations
import kotlin.reflect.jvm.internal.impl.renderer.DescriptorRenderer
import kotlin.reflect.jvm.internal.impl.renderer.DescriptorRendererOptions
import kotlin.reflect.jvm.internal.impl.types.typeUtil.builtIns

open class DynamicTypesSettings {
    open val dynamicTypesAllowed: Boolean
        get() = false
}

class DynamicTypesAllowed: DynamicTypesSettings() {
    override val dynamicTypesAllowed: Boolean
        get() = true
}

fun KotlinType.isDynamic(): Boolean = unwrap() is DynamicType

fun createDynamicType(builtIns: KotlinBuiltIns) = DynamicType(builtIns, Annotations.EMPTY)

class DynamicType(builtIns: KotlinBuiltIns, override val annotations: Annotations) : FlexibleType(builtIns.nothingType, builtIns.nullableAnyType) {
    override val delegate: SimpleType get() = upperBound

    // Nullability has no effect on dynamics
    override fun makeNullableAsSpecified(newNullability: Boolean): DynamicType = this

    override val isMarkedNullable: Boolean get() = false

    override fun replaceAnnotations(newAnnotations: Annotations): DynamicType = DynamicType(delegate.builtIns, newAnnotations)

    override fun render(renderer: DescriptorRenderer, options: DescriptorRendererOptions): String = "dynamic"
}
