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

package com.github.chaoswarzh.intellijtaie.analysis.pta.core.cs.element;

import com.github.chaoswarzh.intellijtaie.analysis.pta.core.cs.context.Context;
import com.github.chaoswarzh.intellijtaie.analysis.pta.core.heap.Obj;
import com.github.chaoswarzh.intellijtaie.util.Indexable;

/**
 * Represents context-sensitive objects.
 */
public class CSObj extends AbstractCSElement implements Indexable {

    private final Obj obj;

    private final int index;

    CSObj(Obj obj, Context context, int index) {
        super(context);
        this.obj = obj;
        this.index = index;
    }

    /**
     * @return the abstract object (without context).
     */
    public Obj getObject() {
        return obj;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return context + ":" + obj;
    }
}
