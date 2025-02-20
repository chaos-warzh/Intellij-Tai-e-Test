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

package com.github.chaoswarzh.intellijtaie.analysis.pta.core.solver;

import com.github.chaoswarzh.intellijtaie.analysis.pta.core.heap.Obj;
import com.github.chaoswarzh.intellijtaie.language.classes.JField;
import com.github.chaoswarzh.intellijtaie.util.collection.Maps;
import com.github.chaoswarzh.intellijtaie.util.collection.MultiMap;
import com.github.chaoswarzh.intellijtaie.util.collection.TwoKeyMultiMap;

import java.util.Set;

/**
 * This {@link ParamProvider} ignored all parameters.
 */
public enum EmptyParamProvider implements ParamProvider {

    INSTANCE;

    public static EmptyParamProvider get() {
        return INSTANCE;
    }

    @Override
    public Set<Obj> getThisObjs() {
        return Set.of();
    }

    @Override
    public Set<Obj> getParamObjs(int i) {
        return Set.of();
    }

    @Override
    public TwoKeyMultiMap<Obj, JField, Obj> getFieldObjs() {
        return Maps.emptyTwoKeyMultiMap();
    }

    @Override
    public MultiMap<Obj, Obj> getArrayObjs() {
        return Maps.emptyMultiMap();
    }
}
