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


package com.github.chaoswarzh.intellijtaie.language.generics;


import java.io.Serializable;

/**
 * In <a href="https://docs.oracle.com/javase/specs/jvms/se20/html/jvms-4.html#jvms-JavaTypeSignature">
 * JVM Spec. 4.7.9.1 JavaTypeSignature</a>,
 * A <i>Java type signature</i> represents either a reference type
 * or a primitive type of the Java programming language.
 * <br>
 * In our implementation, for convenience, {@link VoidDescriptor} is
 * also a {@link TypeGSignature}.
 */
public sealed interface TypeGSignature extends Serializable
        permits ReferenceTypeGSignature, BaseType, VoidDescriptor {
}
