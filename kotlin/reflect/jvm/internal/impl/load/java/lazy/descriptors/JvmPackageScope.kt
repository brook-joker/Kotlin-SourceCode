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

package kotlin.reflect.jvm.internal.impl.load.java.lazy.descriptors

import kotlin.reflect.jvm.internal.impl.descriptors.ClassifierDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.DeclarationDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.PropertyDescriptor
import kotlin.reflect.jvm.internal.impl.descriptors.SimpleFunctionDescriptor
import kotlin.reflect.jvm.internal.impl.incremental.components.LookupLocation
import kotlin.reflect.jvm.internal.impl.incremental.record
import kotlin.reflect.jvm.internal.impl.load.java.lazy.LazyJavaResolverContext
import kotlin.reflect.jvm.internal.impl.load.java.structure.JavaPackage
import kotlin.reflect.jvm.internal.impl.name.Name
import kotlin.reflect.jvm.internal.impl.resolve.scopes.DescriptorKindFilter
import kotlin.reflect.jvm.internal.impl.resolve.scopes.MemberScope
import kotlin.reflect.jvm.internal.impl.resolve.scopes.flatMapClassifierNamesOrNull
import kotlin.reflect.jvm.internal.impl.storage.getValue
import kotlin.reflect.jvm.internal.impl.util.collectionUtils.getFirstClassifierDiscriminateHeaders
import kotlin.reflect.jvm.internal.impl.util.collectionUtils.getFromAllScopes
import kotlin.reflect.jvm.internal.impl.utils.Printer

class JvmPackageScope(
        private val c: LazyJavaResolverContext,
        jPackage: JavaPackage,
        private val packageFragment: LazyJavaPackageFragment
): MemberScope {
    internal val javaScope = LazyJavaPackageScope(c, jPackage, packageFragment)

    private val kotlinScopes by c.storageManager.createLazyValue {
        packageFragment.binaryClasses.values.mapNotNull { partClass ->
            c.components.deserializedDescriptorResolver.createKotlinPackagePartScope(packageFragment, partClass)
        }.toList()
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        recordLookup(name, location)

        val javaClassifier = javaScope.getContributedClassifier(name, location)
        if (javaClassifier != null) return javaClassifier

        return getFirstClassifierDiscriminateHeaders(kotlinScopes) { it.getContributedClassifier(name, location) }
    }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        recordLookup(name, location)
        return getFromAllScopes(javaScope, kotlinScopes) { it.getContributedVariables(name, location) }
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        recordLookup(name, location)
        return getFromAllScopes(javaScope, kotlinScopes) { it.getContributedFunctions(name, location) }
    }

    override fun getContributedDescriptors(
            kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> =
            getFromAllScopes(javaScope, kotlinScopes) { it.getContributedDescriptors(kindFilter, nameFilter) }

    override fun getFunctionNames() = kotlinScopes.flatMapTo(mutableSetOf()) { it.getFunctionNames() }.apply {
        addAll(javaScope.getFunctionNames())
    }
    override fun getVariableNames() = kotlinScopes.flatMapTo(mutableSetOf()) { it.getVariableNames() }.apply {
        addAll(javaScope.getVariableNames())
    }

    override fun getClassifierNames(): Set<Name>? = kotlinScopes.flatMapClassifierNamesOrNull()?.apply {
        addAll(javaScope.getClassifierNames())
    }

    override fun printScopeStructure(p: Printer) {
        p.println(this::class.java.simpleName, " {")
        p.pushIndent()

        p.println("containingDeclaration: $packageFragment")
        javaScope.printScopeStructure(p)

        for (kotlinScope in kotlinScopes) {
            kotlinScope.printScopeStructure(p)
        }

        p.popIndent()
        p.println("}")
    }

    override fun recordLookup(name: Name, location: LookupLocation) {
        c.components.lookupTracker.record(location, packageFragment, name)
    }
}
