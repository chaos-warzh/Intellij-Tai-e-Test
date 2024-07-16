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

package com.github.chaoswarzh.intellijtaie.analysis.dataflow.analysis;

import com.github.chaoswarzh.intellijtaie.analysis.MethodAnalysis;
import com.github.chaoswarzh.intellijtaie.analysis.dataflow.fact.DataflowResult;
import com.github.chaoswarzh.intellijtaie.analysis.dataflow.solver.Solver;
import com.github.chaoswarzh.intellijtaie.analysis.graph.cfg.CFG;
import com.github.chaoswarzh.intellijtaie.analysis.graph.cfg.CFGBuilder;
import com.github.chaoswarzh.intellijtaie.config.AnalysisConfig;
import com.github.chaoswarzh.intellijtaie.ir.IR;

/**
 * Driver for performing a specific kind of data-flow analysis for a method.
 */
public abstract class AnalysisDriver<Node, Fact>
        extends MethodAnalysis<DataflowResult<Node, Fact>> {

    protected AnalysisDriver(AnalysisConfig config) {
        super(config);
    }

    @Override
    public DataflowResult<Node, Fact> analyze(IR ir) {
        CFG<Node> cfg = ir.getResult(CFGBuilder.ID);
        DataflowAnalysis<Node, Fact> analysis = makeAnalysis(cfg);
        Solver<Node, Fact> solver = Solver.getSolver();
        return solver.solve(analysis);
    }

    /**
     * Creates an analysis object for given cfg.
     */
    protected abstract DataflowAnalysis<Node, Fact> makeAnalysis(CFG<Node> cfg);
}
