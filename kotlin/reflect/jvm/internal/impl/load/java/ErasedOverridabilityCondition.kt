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

package kotlin.reflect.jvm.internal.impl.load.java

import kotlin.reflect.jvm.internal.impl.descriptors.CallableDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.ClassDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.SimpleFunctionDescriptor
import kotlin.reflect.jvm.internal.impl.load.java.descriptors.JavaMethodDescriptor
import kotlin.reflect.jvm.internal.impl.load.java.lazy.types.RawSubstitution
import kotlin.reflect.jvm.internal.impl.load.java.lazy.types.RawTypeImpl
import kotlin.reflect.jvm.internal.impl.resolve.ExternalOverridabilityCondition
import kotlin.reflect.jvm.internal.impl.resolve.ExternalOverridabilityCondition.Result
import kotlin.reflect.jvm.internal.impl.resolve.OverridingUtil

class ErasedOverridabilityCondition : ExternalOverridabilityCondition {
    override fun isOverridable(superDescriptor: CallableDescriptor, subDescriptor: CallableDescriptor, subClassDescriptor: ClassDescriptor?): Result {
        if (subDescriptor !is JavaMethodDescriptor || subDescriptor.typeParameters.isNotEmpty()) return Result.UNKNOWN

        val basicOverridability = OverridingUtil.getBasicOverridabilityProblem(superDescriptor, subDescriptor)?.result
        if (basicOverridability != null) return Result.UNKNOWN

        val signatureTypes = subDescriptor.valueParameters.asSequence().map { it.type } +
                             subDescriptor.returnType!! +
                             listOfNotNull(subDescriptor.extensionReceiverParameter?.type)

        if (signatureTypes.any { it.arguments.isNotEmpty() && it.unwrap() !is RawTypeImpl }) return Result.UNKNOWN

        var erasedSuper = superDescriptor.substitute(RawSubstitution.buildSubstitutor()) ?: return Result.UNKNOWN

        if (erasedSuper is SimpleFunctionDescriptor && erasedSuper.typeParameters.isNotEmpty()) {
            // Only simple functions are supported now for erased overrides
            erasedSuper = erasedSuper.newCopyBuilder().setTypeParameters(emptyList()).build()!!
        }

        val overridabilityResult =
                OverridingUtil.DEFAULT.isOverridableByWithoutExternalConditions(erasedSuper, subDescriptor, false).result
        return when (overridabilityResult) {
            OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE -> Result.OVERRIDABLE
            else ->  Result.UNKNOWN
        }
    }

    override fun getContract() = ExternalOverridabilityCondition.Contract.SUCCESS_ONLY
}
