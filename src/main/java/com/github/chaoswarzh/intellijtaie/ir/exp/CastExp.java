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

package com.github.chaoswarzh.intellijtaie.ir.exp;

import com.github.chaoswarzh.intellijtaie.language.type.Type;

import java.util.Set;

/**
 * Representation of cast expression, e.g., (T) o.
 */
public class CastExp implements RValue {

    /**
     * The value to be casted.
     */
    private final Var value;

    private final Type castType;

    public CastExp(Var value, Type castType) {
        this.value = value;
        this.castType = castType;
    }

    public Var getValue() {
        return value;
    }

    public Type getCastType() {
        return castType;
    }

    @Override
    public Type getType() {
        return castType;
    }

    @Override
    public Set<RValue> getUses() {
        return Set.of(value);
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("(%s) %s", castType, value);
    }
}
