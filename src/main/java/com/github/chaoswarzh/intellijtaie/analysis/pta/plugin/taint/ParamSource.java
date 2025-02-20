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

package com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.taint;

import com.github.chaoswarzh.intellijtaie.language.classes.JMethod;
import com.github.chaoswarzh.intellijtaie.language.type.Type;

/**
 * Represents sources which generate taint objects on method parameters.
 *
 * @param method   the method whose parameter are tainted.
 *                 Usually, such methods are program entry points that
 *                 receive inputs (treated as taints).
 * @param indexRef the index of the tainted reference.
 * @param type     the type of the generated taint object.
 * @param rawEntry the raw entry in the taint configuration file
 */
record ParamSource(JMethod method, IndexRef indexRef, Type type, String rawEntry)
        implements Source {

    @Override
    public String toString() {
        return String.format("ParamSource{%s/%s(%s)}", method, indexRef, type);
    }
}
