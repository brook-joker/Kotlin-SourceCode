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

package kotlin.reflect.jvm.internal.impl.builtins

import kotlin.reflect.jvm.internal.impl.descriptors.*
import kotlin.reflect.jvm.internal.impl.descriptors.CallableMemberDescriptor.Kind.DECLARATION
import kotlin.reflect.jvm.internal.impl.descriptors.annotations.Annotations
import kotlin.reflect.jvm.internal.impl.descriptors.impl.SimpleFunctionDescriptorImpl
import kotlin.reflect.jvm.internal.impl.name.Name
import kotlin.reflect.jvm.internal.impl.resolve.descriptorUtil.builtIns
import kotlin.reflect.jvm.internal.impl.resolve.scopes.GivenFunctionsMemberScope
import kotlin.reflect.jvm.internal.impl.storage.StorageManager

class CloneableClassScope(
        storageManager: StorageManager,
        containingClass: ClassDescriptor
) : GivenFunctionsMemberScope(storageManager, containingClass) {
    override fun computeDeclaredFunctions(): List<FunctionDescriptor> = listOf(
            SimpleFunctionDescriptorImpl.create(containingClass, Annotations.EMPTY, CLONE_NAME, DECLARATION, SourceElement.NO_SOURCE).apply {
                initialize(
                        null, containingClass.thisAsReceiverParameter, emptyList(), emptyList(), containingClass.builtIns.anyType,
                        Modality.OPEN, Visibilities.PROTECTED
                )
            }
    )

    companion object {
        internal val CLONE_NAME = Name.identifier("clone")
    }
}
