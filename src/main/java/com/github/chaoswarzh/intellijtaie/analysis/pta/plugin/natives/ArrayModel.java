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

package com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.natives;

import com.github.chaoswarzh.intellijtaie.analysis.pta.core.solver.Solver;
import com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.util.IRModelPlugin;
import com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.util.InvokeHandler;
import com.github.chaoswarzh.intellijtaie.ir.exp.ArrayAccess;
import com.github.chaoswarzh.intellijtaie.ir.exp.CastExp;
import com.github.chaoswarzh.intellijtaie.ir.exp.Var;
import com.github.chaoswarzh.intellijtaie.ir.stmt.Cast;
import com.github.chaoswarzh.intellijtaie.ir.stmt.Copy;
import com.github.chaoswarzh.intellijtaie.ir.stmt.Invoke;
import com.github.chaoswarzh.intellijtaie.ir.stmt.LoadArray;
import com.github.chaoswarzh.intellijtaie.ir.stmt.Stmt;
import com.github.chaoswarzh.intellijtaie.ir.stmt.StoreArray;
import com.github.chaoswarzh.intellijtaie.language.classes.ClassNames;
import com.github.chaoswarzh.intellijtaie.language.classes.JMethod;
import com.github.chaoswarzh.intellijtaie.language.type.ArrayType;
import com.github.chaoswarzh.intellijtaie.language.type.ClassType;
import com.github.chaoswarzh.intellijtaie.language.type.Type;

import java.util.List;

public class ArrayModel extends IRModelPlugin {

    private final ClassType objType;

    private final ArrayType objArrayType;

    /**
     * Counter for naming temporary variables.
     */
    private int counter = 0;

    ArrayModel(Solver solver) {
        super(solver);
        objType = typeSystem.getClassType(ClassNames.OBJECT);
        objArrayType = typeSystem.getArrayType(objType, 1);
    }

    @InvokeHandler(signature = "<java.util.Arrays: java.lang.Object[] copyOf(java.lang.Object[],int)>")
    public List<Stmt> arraysCopyOf(Invoke invoke) {
        Var result = invoke.getResult();
        return result != null
                ? List.of(new Copy(result, invoke.getInvokeExp().getArg(0)))
                : List.of();
    }

    @InvokeHandler(signature = "<java.lang.System: void arraycopy(java.lang.Object,int,java.lang.Object,int,int)>")
    public List<Stmt> systemArraycopy(Invoke invoke) {
        JMethod container = invoke.getContainer();
        Var src = getTempVar(container, "src", objArrayType);
        Var dest = getTempVar(container, "dest", objArrayType);
        Var temp = getTempVar(container, "temp", objType);
        List<Var> args = invoke.getInvokeExp().getArgs();
        return List.of(
                new Cast(src, new CastExp(args.get(0), objArrayType)),
                new Cast(dest, new CastExp(args.get(2), objArrayType)),
                new LoadArray(temp, new ArrayAccess(src, args.get(1))),
                new StoreArray(new ArrayAccess(dest, args.get(3)), temp));
    }

    private Var getTempVar(JMethod container, String name, Type type) {
        String varName = "%native-arraycopy-" + name + counter++;
        return new Var(container, varName, type, -1);
    }
}
