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

package kotlin.reflect.jvm.internal.impl.load.kotlin

import kotlin.reflect.jvm.internal.impl.descriptors.ModuleDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.NotFoundClasses
import kotlin.reflect.jvm.internal.impl.descriptors.deserialization.AdditionalClassPartsProvider
import kotlin.reflect.jvm.internal.impl.descriptors.deserialization.PlatformDependentDeclarationFilter
import kotlin.reflect.jvm.internal.impl.incremental.components.LookupTracker
import kotlin.reflect.jvm.internal.impl.load.java.lazy.LazyJavaPackageFragmentProvider
import kotlin.reflect.jvm.internal.impl.platform.JvmBuiltIns
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.*
import kotlin.reflect.jvm.internal.impl.serialization.jvm.JvmProtoBufUtil
import kotlin.reflect.jvm.internal.impl.storage.StorageManager

// This class is needed only for easier injection: exact types of needed components are specified in the constructor here.
// Otherwise injector generator is not smart enough to deduce, for example, which package fragment provider DeserializationComponents needs
class DeserializationComponentsForJava(
        storageManager: StorageManager,
        moduleDescriptor: ModuleDescriptor,
        configuration: DeserializationConfiguration,
        classDataFinder: JavaClassDataFinder,
        annotationAndConstantLoader: BinaryClassAnnotationAndConstantLoaderImpl,
        packageFragmentProvider: LazyJavaPackageFragmentProvider,
        notFoundClasses: NotFoundClasses,
        errorReporter: ErrorReporter,
        lookupTracker: LookupTracker,
        contractDeserializer: ContractDeserializer
) {
    val components: DeserializationComponents

    init {
        // currently built-ins may be not an instance of JvmBuiltIns only in case of built-ins serialization
        val jvmBuiltIns = moduleDescriptor.builtIns as? JvmBuiltIns
        components = DeserializationComponents(
                storageManager, moduleDescriptor, configuration, classDataFinder, annotationAndConstantLoader, packageFragmentProvider,
                LocalClassifierTypeSettings.Default, errorReporter, lookupTracker, JavaFlexibleTypeDeserializer,
                emptyList(), notFoundClasses, contractDeserializer,
                additionalClassPartsProvider = jvmBuiltIns?.settings ?: AdditionalClassPartsProvider.None,
                platformDependentDeclarationFilter = jvmBuiltIns?.settings ?: PlatformDependentDeclarationFilter.NoPlatformDependent,
                extensionRegistryLite = JvmProtoBufUtil.EXTENSION_REGISTRY
        )
    }
}
