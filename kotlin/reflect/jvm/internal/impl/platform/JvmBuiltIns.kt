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

package kotlin.reflect.jvm.internal.impl.platform

import kotlin.reflect.jvm.internal.impl.builtins.JvmBuiltInClassDescriptorFactory
import kotlin.reflect.jvm.internal.impl.builtins.KotlinBuiltIns
import kotlin.reflect.jvm.internal.impl.descriptors.ModuleDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.deserialization.AdditionalClassPartsProvider
import kotlin.reflect.jvm.internal.impl.descriptors.deserialization.PlatformDependentDeclarationFilter
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmBuiltInsSettings
import kotlin.reflect.jvm.internal.impl.storage.StorageManager
import kotlin.reflect.jvm.internal.impl.storage.getValue
import kotlin.reflect.jvm.internal.impl.utils.sure

class JvmBuiltIns @JvmOverloads constructor(
        storageManager: StorageManager,
        loadBuiltInsFromCurrentClassLoader: Boolean = true
) : KotlinBuiltIns(storageManager) {
    // Module containing JDK classes or having them among dependencies
    private var ownerModuleDescriptor: ModuleDescriptor? = null
    private var isAdditionalBuiltInsFeatureSupported: Boolean = true

    fun initialize(moduleDescriptor: ModuleDescriptor, isAdditionalBuiltInsFeatureSupported: Boolean) {
        assert(ownerModuleDescriptor == null) { "JvmBuiltins repeated initialization" }
        this.ownerModuleDescriptor = moduleDescriptor
        this.isAdditionalBuiltInsFeatureSupported = isAdditionalBuiltInsFeatureSupported
    }

    val settings: JvmBuiltInsSettings by storageManager.createLazyValue {
        JvmBuiltInsSettings(
                builtInsModule, storageManager,
                { ownerModuleDescriptor.sure { "JvmBuiltins has not been initialized properly" } },
                {
                    ownerModuleDescriptor.sure { "JvmBuiltins has not been initialized properly" }
                    isAdditionalBuiltInsFeatureSupported
                }
        )
    }

    init {
        if (loadBuiltInsFromCurrentClassLoader) {
            createBuiltInsModule()
        }
    }

    override fun getPlatformDependentDeclarationFilter(): PlatformDependentDeclarationFilter = settings

    override fun getAdditionalClassPartsProvider(): AdditionalClassPartsProvider = settings

    override fun getClassDescriptorFactories() =
            super.getClassDescriptorFactories() + JvmBuiltInClassDescriptorFactory(storageManager, builtInsModule)
}
