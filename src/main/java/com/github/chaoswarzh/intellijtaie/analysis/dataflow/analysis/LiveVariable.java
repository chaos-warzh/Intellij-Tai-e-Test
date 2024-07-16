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

import com.github.chaoswarzh.intellijtaie.analysis.dataflow.fact.SetFact;
import com.github.chaoswarzh.intellijtaie.analysis.graph.cfg.CFG;
import com.github.chaoswarzh.intellijtaie.config.AnalysisConfig;
import com.github.chaoswarzh.intellijtaie.ir.exp.Var;
import com.github.chaoswarzh.intellijtaie.ir.stmt.Copy;
import com.github.chaoswarzh.intellijtaie.ir.stmt.Stmt;
import com.github.chaoswarzh.intellijtaie.util.Indexer;
import com.github.chaoswarzh.intellijtaie.util.collection.IndexerBitSet;

/**
 * Implementation of live variable analysis.
 */
public class LiveVariable extends AnalysisDriver<Stmt, SetFact<Var>> {

    public static final String ID = "live-var";

    public LiveVariable(AnalysisConfig config) {
        super(config);
    }

    @Override
    protected Analysis makeAnalysis(CFG<Stmt> cfg) {
        return new Analysis(cfg, getOptions().getBoolean("strongly"));
    }

    private static class Analysis extends AbstractDataflowAnalysis<Stmt, SetFact<Var>> {

        /**
         * Whether enable strongly live variable analysis.
         */
        private final boolean strongly;

        /**
         * Indexer for variables in the IR.
         */
        private final Indexer<Var> varIndexer;

        private Analysis(CFG<Stmt> cfg, boolean strongly) {
            super(cfg);
            this.strongly = strongly;
            this.varIndexer = cfg.getIR().getVarIndexer();
        }

        @Override
        public boolean isForward() {
            return false;
        }

        @Override
        public SetFact<Var> newBoundaryFact() {
            return newInitialFact();
        }

        @Override
        public SetFact<Var> newInitialFact() {
            return new SetFact<>(new IndexerBitSet<>(varIndexer, false));
        }

        @Override
        public void meetInto(SetFact<Var> fact, SetFact<Var> target) {
            target.union(fact);
        }

        @Override
        public boolean transferNode(Stmt stmt, SetFact<Var> in, SetFact<Var> out) {
            SetFact<Var> oldIn = in.copy();
            in.set(out);
            // kill definition in stmt
            stmt.getDef().ifPresent(def -> {
                if (def instanceof Var) {
                    in.remove((Var) def);
                }
            });
            // generate uses in stmt
            if (strongly) {
                // only add strongly live variables
                if (stmt instanceof Copy copy) {
                    // for a Copy statement, say x = y, we consider y as
                    // strongly live only when x is also strongly live
                    Var lVar = copy.getLValue();
                    Var rVar = copy.getRValue();
                    if (out.contains(lVar)) {
                        in.add(rVar);
                    }
                } else {
                    // for non-Copy statements, all used variables
                    // are considered strongly live
                    stmt.getUses().forEach(use -> {
                        if (use instanceof Var) {
                            in.add((Var) use);
                        }
                    });
                }
            } else {
                // add all used variables
                stmt.getUses().forEach(use -> {
                    if (use instanceof Var) {
                        in.add((Var) use);
                    }
                });
            }
            return !in.equals(oldIn);
        }
    }
}
