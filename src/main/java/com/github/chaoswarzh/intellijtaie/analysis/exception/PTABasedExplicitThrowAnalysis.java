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
import com.github.chaoswarzh.intellijtaie.analysis.pta.PointerAnalysis;
import com.github.chaoswarzh.intellijtaie.analysis.pta.PointerAnalysisResult;
import com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.exception.ExceptionAnalysis;
import com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.exception.PTAThrowResult;
import com.github.chaoswarzh.intellijtaie.ir.IR;
import com.github.chaoswarzh.intellijtaie.ir.stmt.Invoke;
import com.github.chaoswarzh.intellijtaie.ir.stmt.Throw;
import com.github.chaoswarzh.intellijtaie.language.type.ClassType;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Analyzes explicit exceptions based on pointer analysis.
 */
class PTABasedExplicitThrowAnalysis implements ExplicitThrowAnalysis {

    private final PTAThrowResult ptaThrowResult;

    PTABasedExplicitThrowAnalysis() {
        PointerAnalysisResult result = World.get().getResult(PointerAnalysis.ID);
        this.ptaThrowResult = result.getResult(ExceptionAnalysis.class.getName());
    }

    @Override
    public void analyze(IR ir, ThrowResult result) {
        ptaThrowResult.getResult(ir.getMethod())
                .ifPresent(ptaResult ->
                        ir.forEach(stmt -> {
                            Set<ClassType> exceptions = ptaResult
                                    .mayThrowExplicitly(stmt)
                                    .stream()
                                    .map(o -> (ClassType) o.getType())
                                    .collect(Collectors.toUnmodifiableSet());
                            if (!exceptions.isEmpty()) {
                                if (stmt instanceof Throw) {
                                    result.addExplicit((Throw) stmt, exceptions);
                                } else if (stmt instanceof Invoke) {
                                    result.addExplicit((Invoke) stmt, exceptions);
                                }
                            }
                        })
                );
    }
}
