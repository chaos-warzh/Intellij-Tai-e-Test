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

package com.github.chaoswarzh.intellijtaie.analysis.exception;

import com.github.chaoswarzh.intellijtaie.World;
import com.github.chaoswarzh.intellijtaie.ir.exp.ArithmeticExp;
import com.github.chaoswarzh.intellijtaie.ir.exp.ArrayLengthExp;
import com.github.chaoswarzh.intellijtaie.ir.exp.NewInstance;
import com.github.chaoswarzh.intellijtaie.ir.stmt.Binary;
import com.github.chaoswarzh.intellijtaie.ir.stmt.Cast;
import com.github.chaoswarzh.intellijtaie.ir.stmt.Invoke;
import com.github.chaoswarzh.intellijtaie.ir.stmt.LoadArray;
import com.github.chaoswarzh.intellijtaie.ir.stmt.LoadField;
import com.github.chaoswarzh.intellijtaie.ir.stmt.Monitor;
import com.github.chaoswarzh.intellijtaie.ir.stmt.New;
import com.github.chaoswarzh.intellijtaie.ir.stmt.Stmt;
import com.github.chaoswarzh.intellijtaie.ir.stmt.StmtVisitor;
import com.github.chaoswarzh.intellijtaie.ir.stmt.StoreArray;
import com.github.chaoswarzh.intellijtaie.ir.stmt.StoreField;
import com.github.chaoswarzh.intellijtaie.ir.stmt.Throw;
import com.github.chaoswarzh.intellijtaie.ir.stmt.Unary;
import com.github.chaoswarzh.intellijtaie.language.classes.ClassNames;
import com.github.chaoswarzh.intellijtaie.language.type.ClassType;
import com.github.chaoswarzh.intellijtaie.language.type.TypeSystem;

import java.util.Set;

class ImplicitThrowAnalysis {

    // Implicit exception groups
    private final Set<ClassType> ARITHMETIC_EXCEPTION;

    private final Set<ClassType> LOAD_ARRAY_EXCEPTIONS;

    private final Set<ClassType> STORE_ARRAY_EXCEPTIONS;

    private final Set<ClassType> INITIALIZER_ERROR;

    private final Set<ClassType> CLASS_CAST_EXCEPTION;

    private final Set<ClassType> NEW_ARRAY_EXCEPTIONS;

    private final Set<ClassType> NULL_POINTER_EXCEPTION;

    private final Set<ClassType> OUT_OF_MEMORY_ERROR;

    /**
     * Visitor for compute implicit exceptions that may be thrown by each Stmt.
     */
    private final StmtVisitor<Set<ClassType>> implicitVisitor
            = new StmtVisitor<>() {
        @Override
        public Set<ClassType> visit(New stmt) {
            return stmt.getRValue() instanceof NewInstance ?
                    OUT_OF_MEMORY_ERROR : NEW_ARRAY_EXCEPTIONS;
        }

        @Override
        public Set<ClassType> visit(LoadArray stmt) {
            return LOAD_ARRAY_EXCEPTIONS;
        }

        @Override
        public Set<ClassType> visit(StoreArray stmt) {
            return STORE_ARRAY_EXCEPTIONS;
        }

        @Override
        public Set<ClassType> visit(LoadField stmt) {
            return stmt.isStatic() ?
                    INITIALIZER_ERROR : NULL_POINTER_EXCEPTION;
        }

        @Override
        public Set<ClassType> visit(StoreField stmt) {
            return stmt.isStatic() ?
                    INITIALIZER_ERROR : NULL_POINTER_EXCEPTION;
        }

        @Override
        public Set<ClassType> visit(Binary stmt) {
            if (stmt.getRValue() instanceof ArithmeticExp) {
                ArithmeticExp.Op op = ((ArithmeticExp) stmt.getRValue())
                        .getOperator();
                if (op == ArithmeticExp.Op.DIV || op == ArithmeticExp.Op.REM) {
                    return ARITHMETIC_EXCEPTION;
                }
            }
            return Set.of();
        }

        @Override
        public Set<ClassType> visit(Unary stmt) {
            return stmt.getRValue() instanceof ArrayLengthExp ?
                    NULL_POINTER_EXCEPTION : Set.of();
        }

        @Override
        public Set<ClassType> visit(Cast stmt) {
            return CLASS_CAST_EXCEPTION;
        }

        @Override
        public Set<ClassType> visit(Invoke stmt) {
            return stmt.isStatic() ?
                    INITIALIZER_ERROR : NULL_POINTER_EXCEPTION;
        }

        @Override
        public Set<ClassType> visit(Throw stmt) {
            return NULL_POINTER_EXCEPTION;
        }

        @Override
        public Set<ClassType> visit(Monitor stmt) {
            return NULL_POINTER_EXCEPTION;
        }

        @Override
        public Set<ClassType> visitDefault(Stmt stmt) {
            return Set.of();
        }
    };

    ImplicitThrowAnalysis() {
        TypeSystem ts = World.get().getTypeSystem();
        ClassType arrayStoreException = ts.getClassType(ClassNames.ARRAY_STORE_EXCEPTION);
        ClassType indexOutOfBoundsException = ts.getClassType(ClassNames.INDEX_OUT_OF_BOUNDS_EXCEPTION);
        ClassType nullPointerException = ts.getClassType(ClassNames.NULL_POINTER_EXCEPTION);
        ClassType outOfMemoryError = ts.getClassType(ClassNames.OUT_OF_MEMORY_ERROR);

        ARITHMETIC_EXCEPTION = Set.of(
                ts.getClassType(ClassNames.ARITHMETIC_EXCEPTION));
        LOAD_ARRAY_EXCEPTIONS = Set.of(
                indexOutOfBoundsException,
                nullPointerException);
        STORE_ARRAY_EXCEPTIONS = Set.of(
                arrayStoreException,
                indexOutOfBoundsException,
                nullPointerException);
        INITIALIZER_ERROR = Set.of(
                ts.getClassType(ClassNames.EXCEPTION_IN_INITIALIZER_ERROR));
        CLASS_CAST_EXCEPTION = Set.of(
                ts.getClassType(ClassNames.CLASS_CAST_EXCEPTION));
        NEW_ARRAY_EXCEPTIONS = Set.of(
                outOfMemoryError,
                ts.getClassType(ClassNames.NEGATIVE_ARRAY_SIZE_EXCEPTION));
        NULL_POINTER_EXCEPTION = Set.of(nullPointerException);
        OUT_OF_MEMORY_ERROR = Set.of(outOfMemoryError);
    }

    Set<ClassType> mayThrowImplicitly(Stmt stmt) {
        return stmt.accept(implicitVisitor);
    }

}
