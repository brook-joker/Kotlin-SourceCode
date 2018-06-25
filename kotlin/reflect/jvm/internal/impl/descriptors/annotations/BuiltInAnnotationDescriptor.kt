/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package kotlin.reflect.jvm.internal.impl.descriptors.annotations

import kotlin.reflect.jvm.internal.impl.builtins.KotlinBuiltIns
import kotlin.reflect.jvm.internal.impl.descriptors.SourceElement
import kotlin.reflect.jvm.internal.impl.name.FqName
import kotlin.reflect.jvm.internal.impl.name.Name
import kotlin.reflect.jvm.internal.impl.resolve.constants.ConstantValue
import kotlin.reflect.jvm.internal.impl.types.KotlinType
import kotlin.LazyThreadSafetyMode.PUBLICATION

class BuiltInAnnotationDescriptor(
        private val builtIns: KotlinBuiltIns,
        override val fqName: FqName,
        override val allValueArguments: Map<Name, ConstantValue<*>>
) : AnnotationDescriptor {
    override val type: KotlinType by lazy(PUBLICATION) {
        builtIns.getBuiltInClassByFqName(fqName).defaultType
    }

    override val source: SourceElement
        get() = SourceElement.NO_SOURCE
}
