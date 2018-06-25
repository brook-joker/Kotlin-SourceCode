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

import kotlin.reflect.jvm.internal.impl.builtins.functions.BuiltInFictitiousFunctionClassFactory
import kotlin.reflect.jvm.internal.impl.descriptors.ModuleDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.NotFoundClasses
import kotlin.reflect.jvm.internal.impl.descriptors.deserialization.AdditionalClassPartsProvider
import kotlin.reflect.jvm.internal.impl.descriptors.deserialization.PlatformDependentDeclarationFilter
import kotlin.reflect.jvm.internal.impl.incremental.components.LookupTracker
import kotlin.reflect.jvm.internal.impl.load.kotlin.KotlinClassFinder
import kotlin.reflect.jvm.internal.impl.name.FqName
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.*
import kotlin.reflect.jvm.internal.impl.storage.StorageManager

class JvmBuiltInsPackageFragmentProvider(
        storageManager: StorageManager,
        finder: KotlinClassFinder,
        moduleDescriptor: ModuleDescriptor,
        notFoundClasses: NotFoundClasses,
        additionalClassPartsProvider: AdditionalClassPartsProvider,
        platformDependentDeclarationFilter: PlatformDependentDeclarationFilter
) : AbstractDeserializedPackageFragmentProvider(storageManager, finder, moduleDescriptor) {
    init {
        components = DeserializationComponents(
                storageManager,
                moduleDescriptor,
                DeserializationConfiguration.Default, // TODO
                DeserializedClassDataFinder(this),
                AnnotationAndConstantLoaderImpl(moduleDescriptor, notFoundClasses, BuiltInSerializerProtocol),
                this,
                LocalClassifierTypeSettings.Default,
                ErrorReporter.DO_NOTHING,
                LookupTracker.DO_NOTHING,
                FlexibleTypeDeserializer.ThrowException,
                listOf(
                        BuiltInFictitiousFunctionClassFactory(storageManager, moduleDescriptor),
                        JvmBuiltInClassDescriptorFactory(storageManager, moduleDescriptor)
                ),
                notFoundClasses,
                ContractDeserializer.DEFAULT,
                additionalClassPartsProvider, platformDependentDeclarationFilter,
                BuiltInSerializerProtocol.extensionRegistry
        )
    }

    override fun findPackage(fqName: FqName): DeserializedPackageFragment? =
            finder.findBuiltInsData(fqName)?.let { inputStream ->
                BuiltInsPackageFragmentImpl(fqName, storageManager, moduleDescriptor, inputStream)
            }
}
