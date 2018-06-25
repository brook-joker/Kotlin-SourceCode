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

import kotlin.reflect.jvm.internal.impl.load.java.lazy.types.RawTypeImpl
import kotlin.reflect.jvm.internal.impl.serialization.ProtoBuf
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.FlexibleTypeDeserializer
import kotlin.reflect.jvm.internal.impl.serialization.jvm.JvmProtoBuf
import kotlin.reflect.jvm.internal.impl.types.ErrorUtils
import kotlin.reflect.jvm.internal.impl.types.KotlinType
import kotlin.reflect.jvm.internal.impl.types.KotlinTypeFactory
import kotlin.reflect.jvm.internal.impl.types.SimpleType

object JavaFlexibleTypeDeserializer : FlexibleTypeDeserializer {
    val id = "kotlin.jvm.PlatformType"

    override fun create(proto: ProtoBuf.Type, flexibleId: String, lowerBound: SimpleType, upperBound: SimpleType): KotlinType {
        if (flexibleId != id) return ErrorUtils.createErrorType("Error java flexible type with id: $flexibleId. ($lowerBound..$upperBound)")
        if (proto.hasExtension(JvmProtoBuf.isRaw)) {
            return RawTypeImpl(lowerBound, upperBound)
        }
        return KotlinTypeFactory.flexibleType(lowerBound, upperBound)
    }
}