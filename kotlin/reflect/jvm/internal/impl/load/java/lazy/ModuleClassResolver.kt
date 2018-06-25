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

import kotlin.reflect.jvm.internal.impl.descriptors.ClassDescriptor
import kotlin.reflect.jvm.internal.impl.load.java.structure.JavaClass
import kotlin.reflect.jvm.internal.impl.resolve.jvm.JavaDescriptorResolver
import javax.inject.Inject

interface ModuleClassResolver {
    fun resolveClass(javaClass: JavaClass): ClassDescriptor?
}

class SingleModuleClassResolver() : ModuleClassResolver {
    override fun resolveClass(javaClass: JavaClass): ClassDescriptor? = resolver.resolveClass(javaClass)

    // component dependency cycle
    lateinit var resolver: JavaDescriptorResolver
        @Inject set
}

class ModuleClassResolverImpl(private val descriptorResolverByJavaClass: (JavaClass) -> JavaDescriptorResolver): ModuleClassResolver {
    override fun resolveClass(javaClass: JavaClass): ClassDescriptor? = descriptorResolverByJavaClass(javaClass).resolveClass(javaClass)
}
