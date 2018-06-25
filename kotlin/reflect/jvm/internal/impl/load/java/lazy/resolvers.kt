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

package kotlin.reflect.jvm.internal.impl.load.java.lazy

import kotlin.reflect.jvm.internal.impl.descriptors.DeclarationDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.TypeParameterDescriptor
import kotlin.reflect.jvm.internal.impl.load.java.lazy.descriptors.LazyJavaTypeParameterDescriptor
import kotlin.reflect.jvm.internal.impl.load.java.structure.JavaTypeParameter
import kotlin.reflect.jvm.internal.impl.load.java.structure.JavaTypeParameterListOwner
import kotlin.reflect.jvm.internal.impl.utils.mapToIndex

interface TypeParameterResolver {
    object EMPTY : TypeParameterResolver {
        override fun resolveTypeParameter(javaTypeParameter: JavaTypeParameter): TypeParameterDescriptor? = null
    }

    fun resolveTypeParameter(javaTypeParameter: JavaTypeParameter): TypeParameterDescriptor?
}

class LazyJavaTypeParameterResolver(
        private val c: LazyJavaResolverContext,
        private val containingDeclaration: DeclarationDescriptor,
        typeParameterOwner: JavaTypeParameterListOwner,
        private val typeParametersIndexOffset: Int
) : TypeParameterResolver {
    private val typeParameters: Map<JavaTypeParameter, Int> = typeParameterOwner.typeParameters.mapToIndex()

    private val resolve = c.storageManager.createMemoizedFunctionWithNullableValues {
        typeParameter: JavaTypeParameter ->
        typeParameters[typeParameter]?.let { index ->
            LazyJavaTypeParameterDescriptor(c.child(this), typeParameter, typeParametersIndexOffset + index, containingDeclaration)
        }
    }

    override fun resolveTypeParameter(javaTypeParameter: JavaTypeParameter): TypeParameterDescriptor? {
        return resolve(javaTypeParameter) ?: c.typeParameterResolver.resolveTypeParameter(javaTypeParameter)
    }
}
