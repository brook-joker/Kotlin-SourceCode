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

package kotlin.reflect.jvm.internal.impl.serialization.deserialization

import kotlin.reflect.jvm.internal.impl.descriptors.*
import kotlin.reflect.jvm.internal.impl.descriptors.annotations.AnnotationDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.annotations.AnnotationWithTarget
import kotlin.reflect.jvm.internal.impl.descriptors.deserialization.AdditionalClassPartsProvider
import kotlin.reflect.jvm.internal.impl.descriptors.deserialization.ClassDescriptorFactory
import kotlin.reflect.jvm.internal.impl.descriptors.deserialization.PlatformDependentDeclarationFilter
import kotlin.reflect.jvm.internal.impl.incremental.components.LookupTracker
import kotlin.reflect.jvm.internal.impl.name.ClassId
import kotlin.reflect.jvm.internal.impl.protobuf.ExtensionRegistryLite
import kotlin.reflect.jvm.internal.impl.resolve.constants.ConstantValue
import kotlin.reflect.jvm.internal.impl.serialization.ProtoBuf
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.descriptors.DeserializedContainerSource
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.descriptors.VersionRequirementTable
import kotlin.reflect.jvm.internal.impl.storage.StorageManager

class DeserializationComponents(
        val storageManager: StorageManager,
        val moduleDescriptor: ModuleDescriptor,
        val configuration: DeserializationConfiguration,
        val classDataFinder: ClassDataFinder,
        val annotationAndConstantLoader: AnnotationAndConstantLoader<AnnotationDescriptor, ConstantValue<*>, AnnotationWithTarget>,
        val packageFragmentProvider: PackageFragmentProvider,
        val localClassifierTypeSettings: LocalClassifierTypeSettings,
        val errorReporter: ErrorReporter,
        val lookupTracker: LookupTracker,
        val flexibleTypeDeserializer: FlexibleTypeDeserializer,
        val fictitiousClassDescriptorFactories: Iterable<ClassDescriptorFactory>,
        val notFoundClasses: NotFoundClasses,
        val contractDeserializer: ContractDeserializer,
        val additionalClassPartsProvider: AdditionalClassPartsProvider = AdditionalClassPartsProvider.None,
        val platformDependentDeclarationFilter: PlatformDependentDeclarationFilter = PlatformDependentDeclarationFilter.All,
        val extensionRegistryLite: ExtensionRegistryLite
) {
    val classDeserializer: ClassDeserializer = ClassDeserializer(this)

    fun deserializeClass(classId: ClassId): ClassDescriptor? = classDeserializer.deserializeClass(classId)

    fun createContext(
            descriptor: PackageFragmentDescriptor,
            nameResolver: NameResolver,
            typeTable: TypeTable,
            versionRequirementTable: VersionRequirementTable,
            containerSource: DeserializedContainerSource?
    ): DeserializationContext =
            DeserializationContext(this, nameResolver, descriptor, typeTable, versionRequirementTable, containerSource,
                                   parentTypeDeserializer = null, typeParameters = listOf())
}


class DeserializationContext(
        val components: DeserializationComponents,
        val nameResolver: NameResolver,
        val containingDeclaration: DeclarationDescriptor,
        val typeTable: TypeTable,
        val versionRequirementTable: VersionRequirementTable,
        val containerSource: DeserializedContainerSource?,
        parentTypeDeserializer: TypeDeserializer?,
        typeParameters: List<ProtoBuf.TypeParameter>
) {
    val typeDeserializer = TypeDeserializer(this, parentTypeDeserializer, typeParameters,
                                            "Deserializer for ${containingDeclaration.name}")

    val memberDeserializer = MemberDeserializer(this)

    val storageManager: StorageManager get() = components.storageManager

    fun childContext(
            descriptor: DeclarationDescriptor,
            typeParameterProtos: List<ProtoBuf.TypeParameter>,
            nameResolver: NameResolver = this.nameResolver,
            typeTable: TypeTable = this.typeTable
    ) = DeserializationContext(
            components, nameResolver, descriptor, typeTable, versionRequirementTable, this.containerSource,
            parentTypeDeserializer = this.typeDeserializer, typeParameters = typeParameterProtos
    )
}
