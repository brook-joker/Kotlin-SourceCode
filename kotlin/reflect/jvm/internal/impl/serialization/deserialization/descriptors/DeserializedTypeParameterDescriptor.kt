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

package kotlin.reflect.jvm.internal.impl.serialization.deserialization.descriptors

import kotlin.reflect.jvm.internal.impl.descriptors.SourceElement
import kotlin.reflect.jvm.internal.impl.descriptors.SupertypeLoopChecker
import kotlin.reflect.jvm.internal.impl.descriptors.annotations.Annotations
import kotlin.reflect.jvm.internal.impl.descriptors.impl.AbstractLazyTypeParameterDescriptor
import kotlin.reflect.jvm.internal.impl.resolve.descriptorUtil.builtIns
import kotlin.reflect.jvm.internal.impl.serialization.ProtoBuf
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.Deserialization
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.DeserializationContext
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.upperBounds
import kotlin.reflect.jvm.internal.impl.types.KotlinType

class DeserializedTypeParameterDescriptor(
        private val c: DeserializationContext,
        private val proto: ProtoBuf.TypeParameter,
        index: Int
) : AbstractLazyTypeParameterDescriptor(
        c.storageManager, c.containingDeclaration, c.nameResolver.getName(proto.name),
        Deserialization.variance(proto.variance), proto.reified, index, SourceElement.NO_SOURCE, SupertypeLoopChecker.EMPTY
) {
    override val annotations = DeserializedAnnotations(c.storageManager) {
        c.components.annotationAndConstantLoader.loadTypeParameterAnnotations(proto, c.nameResolver).toList()
    }

    override fun resolveUpperBounds(): List<KotlinType> {
        val upperBounds = proto.upperBounds(c.typeTable)
        if (upperBounds.isEmpty()) {
            return listOf(this.builtIns.defaultBound)
        }
        return upperBounds.map {
            c.typeDeserializer.type(it, Annotations.EMPTY)
        }
    }

    override fun reportSupertypeLoopError(type: KotlinType) = throw IllegalStateException(
            "There should be no cycles for deserialized type parameters, but found for: $this")
}
