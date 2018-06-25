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

package kotlin.reflect.jvm.internal.impl.types;

import org.jetbrains.annotations.NotNull;
import kotlin.reflect.jvm.internal.impl.builtins.KotlinBuiltIns;
import kotlin.reflect.jvm.internal.impl.descriptors.*;
import kotlin.reflect.jvm.internal.impl.resolve.DescriptorUtils;
import kotlin.reflect.jvm.internal.impl.resolve.descriptorUtil.DescriptorUtilsKt;
import kotlin.reflect.jvm.internal.impl.storage.StorageManager;
import kotlin.reflect.jvm.internal.impl.utils.SmartList;

import java.util.Collection;
import java.util.Collections;

public abstract class AbstractClassTypeConstructor extends AbstractTypeConstructor implements TypeConstructor {
    private int hashCode = 0;

    public AbstractClassTypeConstructor(@NotNull StorageManager storageManager) {
        super(storageManager);
    }

    @Override
    public final int hashCode() {
        int currentHashCode = hashCode;
        if (currentHashCode != 0) return currentHashCode;

        ClassifierDescriptor descriptor = getDeclarationDescriptor();
        if (hasMeaningfulFqName(descriptor)) {
            currentHashCode = DescriptorUtils.getFqName(descriptor).hashCode();
        }
        else {
            currentHashCode = System.identityHashCode(this);
        }
        hashCode = currentHashCode;
        return currentHashCode;
    }

    @NotNull
    @Override
    public abstract ClassDescriptor getDeclarationDescriptor();

    @Override
    public final boolean isFinal() {
        ClassDescriptor descriptor = getDeclarationDescriptor();
        return ModalityKt.isFinalClass(descriptor) && !descriptor.isExpect();
    }

    @NotNull
    @Override
    public KotlinBuiltIns getBuiltIns() {
        return DescriptorUtilsKt.getBuiltIns(getDeclarationDescriptor());
    }

    @Override
    public final boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TypeConstructor)) return false;

        // performance optimization: getFqName is slow method
        if (other.hashCode() != hashCode()) return false;

        // Sometimes we can get two classes from different modules with different counts of type parameters.
        // To avoid problems in type checker we suppose that it is different type constructors.
        if (((TypeConstructor) other).getParameters().size() != getParameters().size()) return false;

        ClassifierDescriptor myDescriptor = getDeclarationDescriptor();
        ClassifierDescriptor otherDescriptor = ((TypeConstructor) other).getDeclarationDescriptor();

        if (!hasMeaningfulFqName(myDescriptor) ||
            otherDescriptor != null && !hasMeaningfulFqName(otherDescriptor)) {
            // All error types and local classes have the same descriptor,
            // but we've already checked identity equality in the beginning of the method
            return false;
        }

        if (otherDescriptor instanceof ClassDescriptor) {
            return areFqNamesEqual(((ClassDescriptor) myDescriptor), ((ClassDescriptor) otherDescriptor));
        }

        return false;
    }

    private static boolean areFqNamesEqual(ClassDescriptor first, ClassDescriptor second) {
        if (!first.getName().equals(second.getName())) return false;

        DeclarationDescriptor a = first.getContainingDeclaration();
        DeclarationDescriptor b = second.getContainingDeclaration();
        while (a != null && b != null) {
            if (a instanceof ModuleDescriptor) return b instanceof ModuleDescriptor;
            if (b instanceof ModuleDescriptor) return false;

            if (a instanceof PackageFragmentDescriptor) {
                return b instanceof PackageFragmentDescriptor &&
                       ((PackageFragmentDescriptor) a).getFqName().equals(((PackageFragmentDescriptor) b).getFqName());
            }
            if (b instanceof PackageFragmentDescriptor) return false;

            if (!a.getName().equals(b.getName())) return false;

            a = a.getContainingDeclaration();
            b = b.getContainingDeclaration();
        }
        return true;
    }

    private static boolean hasMeaningfulFqName(@NotNull ClassifierDescriptor descriptor) {
        return !ErrorUtils.isError(descriptor) &&
               !DescriptorUtils.isLocal(descriptor);
    }

    @NotNull
    @Override
    protected Collection<KotlinType> getAdditionalNeighboursInSupertypeGraph(boolean useCompanions) {
        DeclarationDescriptor containingDeclaration = getDeclarationDescriptor().getContainingDeclaration();

        if (!(containingDeclaration instanceof ClassDescriptor)) {
            return Collections.emptyList();
        }

        Collection<KotlinType> additionalNeighbours = new SmartList<KotlinType>();

        // We suppose that there is an edge from C to A in graph when disconnecting loops in supertypes,
        // because such cyclic declarations should be prohibited (see p.10.2.1 of Kotlin spec)
        // class A : B {
        //   static class C {}
        // }
        // class B : A.C {}
        ClassDescriptor containingClassDescriptor = (ClassDescriptor) containingDeclaration;
        additionalNeighbours.add(containingClassDescriptor.getDefaultType());

        // Also we add edge from host-class to companion object. Together with previous edges
        // (from nesteds to containing class), they can create visibility loops like in the
        // following example:
        //
        // class ContainingClass {
        //   open class Nested {}  // to create scope for resolving Nested, we have to resolve CO header
        //   companion object : Nested() {} // to resolve CO header, we have to resolve Nested
        // }
        //
        // Relates to KT-21515
        ClassDescriptor companion = containingClassDescriptor.getCompanionObjectDescriptor();
        if (useCompanions && companion != null) {
            additionalNeighbours.add(companion.getDefaultType());
        }

        return additionalNeighbours;
    }
}
