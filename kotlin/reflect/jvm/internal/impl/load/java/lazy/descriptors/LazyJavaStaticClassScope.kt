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

package kotlin.reflect.jvm.internal.impl.load.java.lazy.descriptors

import kotlin.reflect.jvm.internal.impl.descriptors.ClassDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.ClassifierDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.PropertyDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.SimpleFunctionDescriptor
import kotlin.reflect.jvm.internal.impl.incremental.components.LookupLocation
import kotlin.reflect.jvm.internal.impl.incremental.components.NoLookupLocation
import kotlin.reflect.jvm.internal.impl.load.java.components.DescriptorResolverUtils.resolveOverridesForStaticMembers
import kotlin.reflect.jvm.internal.impl.load.java.descriptors.getParentJavaStaticClassScope
import kotlin.reflect.jvm.internal.impl.load.java.lazy.LazyJavaResolverContext
import kotlin.reflect.jvm.internal.impl.load.java.structure.JavaClass
import kotlin.reflect.jvm.internal.impl.name.Name
import kotlin.reflect.jvm.internal.impl.resolve.DescriptorFactory.createEnumValueOfMethod
import kotlin.reflect.jvm.internal.impl.resolve.DescriptorFactory.createEnumValuesMethod
import kotlin.reflect.jvm.internal.impl.resolve.DescriptorUtils
import kotlin.reflect.jvm.internal.impl.resolve.scopes.DescriptorKindFilter
import kotlin.reflect.jvm.internal.impl.resolve.scopes.MemberScope
import kotlin.reflect.jvm.internal.impl.utils.DFS

class LazyJavaStaticClassScope(
        c: LazyJavaResolverContext,
        private val jClass: JavaClass,
        override val ownerDescriptor: LazyJavaClassDescriptor
) : LazyJavaStaticScope(c) {

    override fun computeMemberIndex() = ClassDeclaredMemberIndex(jClass) { it.isStatic }

    override fun computeFunctionNames(kindFilter: DescriptorKindFilter, nameFilter: ((Name) -> Boolean)?) =
        declaredMemberIndex().getMethodNames().toMutableSet().apply {
            addAll(ownerDescriptor.getParentJavaStaticClassScope()?.getFunctionNames().orEmpty())
            if (jClass.isEnum) {
                addAll(listOf(DescriptorUtils.ENUM_VALUE_OF, DescriptorUtils.ENUM_VALUES))
            }
        }

    override fun computePropertyNames(kindFilter: DescriptorKindFilter, nameFilter: ((Name) -> Boolean)?) =
        declaredMemberIndex().getFieldNames().toMutableSet().apply {
            flatMapJavaStaticSupertypesScopes(ownerDescriptor, this) { it.getVariableNames() }
        }

    override fun computeClassNames(kindFilter: DescriptorKindFilter, nameFilter: ((Name) -> Boolean)?): Set<Name> = emptySet()

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        // We don't need to track lookups here because we find nested/inner classes in LazyJavaClassMemberScope
        return null
    }

    override fun computeNonDeclaredFunctions(result: MutableCollection<SimpleFunctionDescriptor>, name: Name) {
        val functionsFromSupertypes = getStaticFunctionsFromJavaSuperClasses(name, ownerDescriptor)
        result.addAll(resolveOverridesForStaticMembers(name, functionsFromSupertypes, result, ownerDescriptor, c.components.errorReporter))

        if (jClass.isEnum) {
            when (name) {
                DescriptorUtils.ENUM_VALUE_OF -> result.add(createEnumValueOfMethod(ownerDescriptor))
                DescriptorUtils.ENUM_VALUES -> result.add(createEnumValuesMethod(ownerDescriptor))
            }
        }
    }

    override fun computeNonDeclaredProperties(name: Name, result: MutableCollection<PropertyDescriptor>) {
        val propertiesFromSupertypes = flatMapJavaStaticSupertypesScopes(ownerDescriptor, mutableSetOf()) {
            it.getContributedVariables(name, NoLookupLocation.WHEN_GET_SUPER_MEMBERS)
        }

        if (result.isNotEmpty()) {
            result.addAll(resolveOverridesForStaticMembers(
                    name, propertiesFromSupertypes, result, ownerDescriptor, c.components.errorReporter
            ))
        }
        else {
            result.addAll(propertiesFromSupertypes.groupBy {
                it.realOriginal
            }.flatMap {
                resolveOverridesForStaticMembers(name, it.value, result, ownerDescriptor, c.components.errorReporter)
            })
        }
    }

    private fun getStaticFunctionsFromJavaSuperClasses(name: Name, descriptor: ClassDescriptor): Set<SimpleFunctionDescriptor> {
        val staticScope = descriptor.getParentJavaStaticClassScope() ?: return emptySet()
        return staticScope.getContributedFunctions(name, NoLookupLocation.WHEN_GET_SUPER_MEMBERS).toSet()
    }

    private fun <R> flatMapJavaStaticSupertypesScopes(
            root: ClassDescriptor,
            result: MutableSet<R>,
            onJavaStaticScope: (MemberScope) -> Collection<R>
    ): Set<R> {
        DFS.dfs(listOf(root),
                {
                    it.typeConstructor.supertypes.asSequence().mapNotNull {
                        supertype -> supertype.constructor.declarationDescriptor as? ClassDescriptor
                    }.asIterable()
                },
                object : DFS.AbstractNodeHandler<ClassDescriptor, Unit>() {
                    override fun beforeChildren(current: ClassDescriptor): Boolean {
                        if (current === root) return true
                        val staticScope = current.staticScope

                        if (staticScope is LazyJavaStaticScope) {
                            result.addAll(onJavaStaticScope(staticScope))
                            return false
                        }
                        return true
                    }

                    override fun result() {}
                }
        )

        return result
    }

    private val PropertyDescriptor.realOriginal: PropertyDescriptor
        get() {
            if (this.kind.isReal) return this

            return this.overriddenDescriptors.map { it.realOriginal }.distinct().single()
        }
}
