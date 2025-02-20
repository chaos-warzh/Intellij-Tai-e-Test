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

package com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.util;

import com.github.chaoswarzh.intellijtaie.analysis.pta.core.solver.Solver;
import com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * Provides common functionalities for implementing the plugins which model APIs.
 *
 * @see InvokeHandler
 */
abstract class ModelPlugin extends SolverHolder implements Plugin {

    protected ModelPlugin(Solver solver) {
        super(solver);
    }

    protected void registerHandlers() {
        Class<?> clazz = getClass();
        for (Method method : clazz.getMethods()) {
            InvokeHandler[] invokeHandlers = method.getAnnotationsByType(InvokeHandler.class);
            if (invokeHandlers != null) {
                for (InvokeHandler invokeHandler : invokeHandlers) {
                    registerHandler(invokeHandler, method);
                }
            }
        }
    }

    protected abstract void registerHandler(InvokeHandler invokeHandler, Method handler);
}
