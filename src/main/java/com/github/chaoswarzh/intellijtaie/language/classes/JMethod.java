/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.chaoswarzh.intellijtaie.language.classes;

import com.github.chaoswarzh.intellijtaie.World;
import com.github.chaoswarzh.intellijtaie.ir.IR;
import com.github.chaoswarzh.intellijtaie.ir.proginfo.MethodRef;
import com.github.chaoswarzh.intellijtaie.language.annotation.Annotation;
import com.github.chaoswarzh.intellijtaie.language.annotation.AnnotationHolder;
import com.github.chaoswarzh.intellijtaie.language.generics.MethodGSignature;
import com.github.chaoswarzh.intellijtaie.language.type.ClassType;
import com.github.chaoswarzh.intellijtaie.language.type.Type;
import com.github.chaoswarzh.intellijtaie.util.AnalysisException;
import com.github.chaoswarzh.intellijtaie.util.Experimental;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Represents methods in the program. Each instance contains various
 * information of a method, including method name, signature, declaring class,
 * method body (IR), etc.
 */
public class JMethod extends ClassMember {

    private final List<Type> paramTypes;

    private final Type returnType;

    private final List<ClassType> exceptions;

    private final Subsignature subsignature;

    @Nullable
    @Experimental
    private final MethodGSignature gSignature;

    @Nullable
    private final List<AnnotationHolder> paramAnnotations;

    @Nullable
    private final List<String> paramNames;

    /**
     * Source of the body (and/or other information) of this method.
     * IRBuilder can use this to build method IR.
     * <br>
     * Notes: This field is {@code transient} because it is not serializable.
     */
    private final transient Object methodSource;

    /**
     * Notes: This field is {@code transient} because it is serialized separately.
     *
     * @see com.github.chaoswarzh.intellijtaie.frontend.cache.CachedIRBuilder
     */
    private transient IR ir;

    public JMethod(JClass declaringClass, String name, Set<Modifier> modifiers,
                   List<Type> paramTypes, Type returnType, List<ClassType> exceptions,
                   @Nullable MethodGSignature gSignature,
                   AnnotationHolder annotationHolder,
                   @Nullable List<AnnotationHolder> paramAnnotations,
                   @Nullable List<String> paramNames,
                   Object methodSource) {
        super(declaringClass, name, modifiers, annotationHolder);
        this.paramTypes = List.copyOf(paramTypes);
        this.returnType = returnType;
        this.exceptions = List.copyOf(exceptions);
        this.signature = StringReps.getSignatureOf(this);
        this.subsignature = Subsignature.get(name, paramTypes, returnType);
        this.gSignature = gSignature;
        this.paramAnnotations = paramAnnotations;
        this.paramNames = paramNames;
        this.methodSource = methodSource;
    }

    public boolean isAbstract() {
        return Modifier.hasAbstract(modifiers);
    }

    public boolean isNative() {
        return Modifier.hasNative(modifiers);
    }

    public boolean isConstructor() {
        return name.equals(MethodNames.INIT);
    }

    public boolean isStaticInitializer() {
        return name.equals(MethodNames.CLINIT);
    }

    public int getParamCount() {
        return paramTypes.size();
    }

    public Type getParamType(int i) {
        return paramTypes.get(i);
    }

    public List<Type> getParamTypes() {
        return paramTypes;
    }

    /**
     * @return {@code true} if the {@code i}-th parameter has annotation
     * of {@code type}.
     */
    public boolean hasParamAnnotation(int i, String type) {
        return paramAnnotations != null &&
                paramAnnotations.get(i).hasAnnotation(type);
    }

    /**
     * @return the annotation attached on the {@code i}-th parameter that is
     * of {@code type}. If such annotation is absent, {@code null} is returned.
     */
    @Nullable
    public Annotation getParamAnnotation(int i, String type) {
        return paramAnnotations == null ? null :
                paramAnnotations.get(i).getAnnotation(type);
    }

    /**
     * @return all annotations attached on the {@code i}-th parameter. If the
     * parameter does not have annotation, an empty collection is returned.
     */
    public Collection<Annotation> getParamAnnotations(int i) {
        return paramAnnotations == null ? Set.of() :
                paramAnnotations.get(i).getAnnotations();
    }

    @Nullable
    public String getParamName(int i) {
        return paramNames == null ? null : paramNames.get(i);
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<ClassType> getExceptions() {
        return exceptions;
    }

    public Subsignature getSubsignature() {
        return subsignature;
    }

    @Nullable
    @Experimental
    public MethodGSignature getGSignature() {
        return gSignature;
    }

    public Object getMethodSource() {
        return methodSource;
    }

    public IR getIR() {
        if (ir == null) {
            if (isAbstract()) {
                throw new AnalysisException("Abstract method " + this +
                        " has no method body");
            }
            if (isNative()) {
                ir = World.get().getNativeModel().buildNativeIR(this);
            } else {
                ir = World.get().getIRBuilder().buildIR(this);
            }
        }
        return ir;
    }

    /**
     * @return the {@link MethodRef} pointing to this method.
     */
    public MethodRef getRef() {
        return MethodRef.get(declaringClass, name,
                paramTypes, returnType, isStatic());
    }
}
