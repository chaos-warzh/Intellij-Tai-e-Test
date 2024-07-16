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

import com.github.chaoswarzh.intellijtaie.analysis.pta.core.cs.context.Context;
import com.github.chaoswarzh.intellijtaie.analysis.pta.core.cs.element.CSMethod;
import com.github.chaoswarzh.intellijtaie.analysis.pta.core.cs.element.CSObj;
import com.github.chaoswarzh.intellijtaie.analysis.pta.core.cs.element.CSVar;
import com.github.chaoswarzh.intellijtaie.ir.IR;
import com.github.chaoswarzh.intellijtaie.ir.exp.Var;
import com.github.chaoswarzh.intellijtaie.language.classes.JMethod;
import com.github.chaoswarzh.intellijtaie.util.collection.Maps;
import com.github.chaoswarzh.intellijtaie.util.collection.MultiMap;

import java.util.function.Predicate;

import static com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.util.InvokeUtils.BASE;

/**
 * Handles sanitizers in taint analysis.
 */
class SanitizerHandler extends OnFlyHandler {

    private final MultiMap<JMethod, ParamSanitizer> paramSanitizers = Maps.newMultiMap();

    /**
     * Used to filter out taint objects from points-to set.
     */
    private final Predicate<CSObj> taintFilter;

    SanitizerHandler(HandlerContext context) {
        super(context);
        taintFilter = o -> !context.manager().isTaint(o.getObject());
        context.config().paramSanitizers()
                .forEach(s -> this.paramSanitizers.put(s.method(), s));
    }

    /**
     *
     * Handles parameter sanitizers.
     */
    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        JMethod method = csMethod.getMethod();
        if (paramSanitizers.containsKey(method)) {
            Context context = csMethod.getContext();
            IR ir = method.getIR();
            paramSanitizers.get(method).forEach(sanitizer -> {
                Var param = getParam(ir, sanitizer.index());
                CSVar csParam = csManager.getCSVar(context, param);
                solver.addPointerFilter(csParam, taintFilter);
            });
        }
    }

    private static Var getParam(IR ir, int index) {
        return switch (index) {
            case BASE -> ir.getThis();
            default -> ir.getParam(index);
        };
    }
}
