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

package kotlin.reflect.jvm.internal.components

import kotlin.reflect.jvm.internal.impl.builtins.ReflectionTypes
import kotlin.reflect.jvm.internal.impl.descriptors.ModuleDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.NotFoundClasses
import kotlin.reflect.jvm.internal.impl.descriptors.SupertypeLoopChecker
import kotlin.reflect.jvm.internal.impl.descriptors.impl.ModuleDescriptorImpl
import kotlin.reflect.jvm.internal.impl.incremental.components.LookupTracker
import kotlin.reflect.jvm.internal.impl.load.java.AnnotationTypeQualifierResolver
import kotlin.reflect.jvm.internal.impl.load.java.JavaClassesTracker
import kotlin.reflect.jvm.internal.impl.load.java.components.*
import kotlin.reflect.jvm.internal.impl.load.java.lazy.JavaResolverComponents
import kotlin.reflect.jvm.internal.impl.load.java.lazy.LazyJavaPackageFragmentProvider
import kotlin.reflect.jvm.internal.impl.load.java.lazy.SingleModuleClassResolver
import kotlin.reflect.jvm.internal.impl.load.java.typeEnhancement.SignatureEnhancement
import kotlin.reflect.jvm.internal.impl.load.kotlin.BinaryClassAnnotationAndConstantLoaderImpl
import kotlin.reflect.jvm.internal.impl.load.kotlin.DeserializationComponentsForJava
import kotlin.reflect.jvm.internal.impl.load.kotlin.DeserializedDescriptorResolver
import kotlin.reflect.jvm.internal.impl.load.kotlin.JavaClassDataFinder
import kotlin.reflect.jvm.internal.impl.name.Name
import kotlin.reflect.jvm.internal.impl.platform.JvmBuiltIns
import kotlin.reflect.jvm.internal.impl.resolve.jvm.JavaDescriptorResolver
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.ContractDeserializer
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.DeserializationComponents
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.DeserializationConfiguration
import kotlin.reflect.jvm.internal.impl.storage.LockBasedStorageManager
import kotlin.reflect.jvm.internal.impl.utils.Jsr305State

class RuntimeModuleData private constructor(
        val deserialization: DeserializationComponents,
        val packagePartProvider: RuntimePackagePartProvider
) {
    val module: ModuleDescriptor get() = deserialization.moduleDescriptor

    companion object {
        fun create(classLoader: ClassLoader): RuntimeModuleData {
            val storageManager = LockBasedStorageManager()
            val builtIns = JvmBuiltIns(storageManager)
            val module = ModuleDescriptorImpl(Name.special("<runtime module for $classLoader>"), storageManager, builtIns)

            val reflectKotlinClassFinder = ReflectKotlinClassFinder(classLoader)
            val deserializedDescriptorResolver = DeserializedDescriptorResolver()
            val singleModuleClassResolver = SingleModuleClassResolver()
            val runtimePackagePartProvider = RuntimePackagePartProvider(classLoader)
            val javaResolverCache = JavaResolverCache.EMPTY
            val notFoundClasses = NotFoundClasses(storageManager, module)
            val annotationTypeQualifierResolver = AnnotationTypeQualifierResolver(storageManager, Jsr305State.DISABLED)
            val globalJavaResolverContext = JavaResolverComponents(
                    storageManager, ReflectJavaClassFinder(classLoader), reflectKotlinClassFinder, deserializedDescriptorResolver,
                    ExternalAnnotationResolver.EMPTY, SignaturePropagator.DO_NOTHING, RuntimeErrorReporter, javaResolverCache,
                    JavaPropertyInitializerEvaluator.DoNothing, SamConversionResolver.Empty, RuntimeSourceElementFactory, singleModuleClassResolver,
                    runtimePackagePartProvider, SupertypeLoopChecker.EMPTY, LookupTracker.DO_NOTHING, module,
                    ReflectionTypes(module, notFoundClasses),
                    annotationTypeQualifierResolver,
                    SignatureEnhancement(annotationTypeQualifierResolver, Jsr305State.DISABLED),
                    JavaClassesTracker.Default
            )

            val lazyJavaPackageFragmentProvider = LazyJavaPackageFragmentProvider(globalJavaResolverContext)

            builtIns.initialize(module, isAdditionalBuiltInsFeatureSupported = true)

            val javaDescriptorResolver = JavaDescriptorResolver(lazyJavaPackageFragmentProvider, javaResolverCache)
            val javaClassDataFinder = JavaClassDataFinder(reflectKotlinClassFinder, deserializedDescriptorResolver)
            val binaryClassAnnotationAndConstantLoader = BinaryClassAnnotationAndConstantLoaderImpl(
                    module, notFoundClasses, storageManager, reflectKotlinClassFinder
            )
            val deserializationComponentsForJava = DeserializationComponentsForJava(
                    storageManager, module, DeserializationConfiguration.Default, javaClassDataFinder,
                    binaryClassAnnotationAndConstantLoader, lazyJavaPackageFragmentProvider, notFoundClasses,
                    RuntimeErrorReporter, LookupTracker.DO_NOTHING, ContractDeserializer.DEFAULT
            )

            singleModuleClassResolver.resolver = javaDescriptorResolver
            deserializedDescriptorResolver.setComponents(deserializationComponentsForJava)

            module.setDependencies(module, builtIns.builtInsModule)
            module.initialize(javaDescriptorResolver.packageFragmentProvider)

            return RuntimeModuleData(deserializationComponentsForJava.components, runtimePackagePartProvider)
        }
    }
}
