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

package kotlin.reflect.jvm.internal.impl.serialization.deserialization

import kotlin.reflect.jvm.internal.impl.descriptors.ModuleDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.impl.PackageFragmentDescriptorImpl
import kotlin.reflect.jvm.internal.impl.name.FqName
import kotlin.reflect.jvm.internal.impl.name.Name
import kotlin.reflect.jvm.internal.impl.resolve.scopes.MemberScope
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.descriptors.DeserializedMemberScope
import kotlin.reflect.jvm.internal.impl.storage.StorageManager
import javax.inject.Inject

abstract class DeserializedPackageFragment(
        fqName: FqName,
        protected val storageManager: StorageManager,
        module: ModuleDescriptor
) : PackageFragmentDescriptorImpl(module, fqName) {
    // component dependency cycle
    @set:Inject
    lateinit var components: DeserializationComponents

    private val memberScope = storageManager.createLazyValue { computeMemberScope() }

    abstract val classDataFinder: ClassDataFinder

    protected abstract fun computeMemberScope(): MemberScope

    override fun getMemberScope() = memberScope()

    open fun hasTopLevelClass(name: Name): Boolean {
        val scope = getMemberScope()
        return scope is DeserializedMemberScope && name in scope.classNames
    }
}
