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

package com.github.chaoswarzh.intellijtaie.ir.stmt;

import com.github.chaoswarzh.intellijtaie.ir.exp.LValue;
import com.github.chaoswarzh.intellijtaie.ir.exp.RValue;
import com.github.chaoswarzh.intellijtaie.util.collection.ArraySet;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.Set;

/**
 * Representation of assign statements.
 *
 * @param <L> type of lvalue.
 * @param <R> type of rvalue.
 */
public abstract class AssignStmt<L extends LValue, R extends RValue>
        extends DefinitionStmt<L, R> {

    private final L lvalue;

    private final R rvalue;

    public AssignStmt(L lvalue, R rvalue) {
        this.lvalue = lvalue;
        this.rvalue = rvalue;
    }

    @Override
    @Nonnull
    public L getLValue() {
        return lvalue;
    }

    @Override
    public R getRValue() {
        return rvalue;
    }

    @Override
    public Optional<LValue> getDef() {
        return Optional.of(lvalue);
    }

    @Override
    public Set<RValue> getUses() {
        Set<RValue> lUses = lvalue.getUses();
        Set<RValue> rUses = rvalue.getUses();
        Set<RValue> uses = new ArraySet<>(lUses.size() + rUses.size() + 1);
        uses.addAll(lUses);
        uses.addAll(rUses);
        uses.add(rvalue);
        return uses;
    }

    @Override
    public String toString() {
        return lvalue + " = " + rvalue;
    }
}
