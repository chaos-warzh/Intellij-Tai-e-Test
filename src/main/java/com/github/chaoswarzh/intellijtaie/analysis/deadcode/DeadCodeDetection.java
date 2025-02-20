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

package com.github.chaoswarzh.intellijtaie.analysis.deadcode;

import com.github.chaoswarzh.intellijtaie.analysis.MethodAnalysis;
import com.github.chaoswarzh.intellijtaie.analysis.dataflow.analysis.LiveVariable;
import com.github.chaoswarzh.intellijtaie.analysis.dataflow.analysis.constprop.CPFact;
import com.github.chaoswarzh.intellijtaie.analysis.dataflow.analysis.constprop.ConstantPropagation;
import com.github.chaoswarzh.intellijtaie.analysis.dataflow.analysis.constprop.Evaluator;
import com.github.chaoswarzh.intellijtaie.analysis.dataflow.analysis.constprop.Value;
import com.github.chaoswarzh.intellijtaie.analysis.dataflow.fact.NodeResult;
import com.github.chaoswarzh.intellijtaie.analysis.dataflow.fact.SetFact;
import com.github.chaoswarzh.intellijtaie.analysis.graph.cfg.CFG;
import com.github.chaoswarzh.intellijtaie.analysis.graph.cfg.CFGBuilder;
import com.github.chaoswarzh.intellijtaie.analysis.graph.cfg.CFGEdge;
import com.github.chaoswarzh.intellijtaie.config.AnalysisConfig;
import com.github.chaoswarzh.intellijtaie.ir.IR;
import com.github.chaoswarzh.intellijtaie.ir.exp.ArithmeticExp;
import com.github.chaoswarzh.intellijtaie.ir.exp.ArrayAccess;
import com.github.chaoswarzh.intellijtaie.ir.exp.CastExp;
import com.github.chaoswarzh.intellijtaie.ir.exp.FieldAccess;
import com.github.chaoswarzh.intellijtaie.ir.exp.NewExp;
import com.github.chaoswarzh.intellijtaie.ir.exp.RValue;
import com.github.chaoswarzh.intellijtaie.ir.exp.Var;
import com.github.chaoswarzh.intellijtaie.ir.stmt.AssignStmt;
import com.github.chaoswarzh.intellijtaie.ir.stmt.If;
import com.github.chaoswarzh.intellijtaie.ir.stmt.Stmt;
import com.github.chaoswarzh.intellijtaie.ir.stmt.SwitchStmt;
import com.github.chaoswarzh.intellijtaie.util.collection.Sets;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.Queue;
import java.util.Set;

/**
 * Detects dead code in an IR.
 */
public class DeadCodeDetection extends MethodAnalysis<Set<Stmt>> {

    public static final String ID = "dead-code";

    public DeadCodeDetection(AnalysisConfig config) {
        super(config);
    }

    @Override
    public Set<Stmt> analyze(IR ir) {
        // obtain results of pre-analyses
        CFG<Stmt> cfg = ir.getResult(CFGBuilder.ID);
        NodeResult<Stmt, CPFact> constants =
                ir.getResult(ConstantPropagation.ID);
        NodeResult<Stmt, SetFact<Var>> liveVars =
                ir.getResult(LiveVariable.ID);
        // keep statements (dead code) sorted in the resulting set
        Set<Stmt> deadCode = Sets.newOrderedSet(Comparator.comparing(Stmt::getIndex));
        // initialize graph traversal
        Set<Stmt> visited = Sets.newSet(cfg.getNumberOfNodes());
        Queue<Stmt> queue = new ArrayDeque<>();
        queue.add(cfg.getEntry());
        while (!queue.isEmpty()) {
            Stmt stmt = queue.remove();
            visited.add(stmt);
            if (isDeadAssignment(stmt, liveVars)) {
                // record dead assignment
                deadCode.add(stmt);
            }
            cfg.getOutEdgesOf(stmt)
                    .stream()
                    .filter(edge -> !isUnreachableBranch(edge, constants))
                    .map(CFGEdge::target)
                    .forEach(succ -> {
                        if (!visited.contains(succ)) {
                            queue.add(succ);
                        }
                    });
        }
        if (visited.size() < cfg.getNumberOfNodes()) {
            // this means that some nodes are not reachable during traversal
            for (Stmt s : ir) {
                if (!visited.contains(s)) {
                    deadCode.add(s);
                }
            }
        }
        return deadCode.isEmpty() ? Collections.emptySet() : deadCode;
    }

    private static boolean isDeadAssignment(
            Stmt stmt, NodeResult<Stmt, SetFact<Var>> liveVars) {
        if (stmt instanceof AssignStmt<?, ?> assign) {
            if (assign.getLValue() instanceof Var lhs) {
                return !liveVars.getOutFact(assign).contains(lhs) &&
                        hasNoSideEffect(assign.getRValue());
            }
        }
        return false;
    }

    private static boolean isUnreachableBranch(
            CFGEdge<Stmt> edge, NodeResult<Stmt, CPFact> constants) {
        Stmt src = edge.source();
        if (src instanceof If ifStmt) {
            Value cond = Evaluator.evaluate(
                    ifStmt.getCondition(), constants.getInFact(ifStmt));
            if (cond.isConstant()) {
                int v = cond.getConstant();
                return v == 1 && edge.getKind() == CFGEdge.Kind.IF_FALSE ||
                        v == 0 && edge.getKind() == CFGEdge.Kind.IF_TRUE;
            }
        } else if (src instanceof SwitchStmt switchStmt) {
            Value condV = Evaluator.evaluate(
                    switchStmt.getVar(), constants.getInFact(switchStmt));
            if (condV.isConstant()) {
                int v = condV.getConstant();
                if (edge.isSwitchCase()) {
                    return v != edge.getCaseValue();
                } else { // default case
                    // if any other case matches the case value, then
                    // default case is unreachable (dead)
                    return switchStmt.getCaseValues()
                            .stream()
                            .anyMatch(x -> x == v);
                }
            }
        }
        return false;
    }

    /**
     * @return true if given RValue has no side effect, otherwise false.
     */
    private static boolean hasNoSideEffect(RValue rvalue) {
        // new expression modifies the heap
        if (rvalue instanceof NewExp ||
                // cast may trigger ClassCastException
                rvalue instanceof CastExp ||
                // static field access may trigger class initialization
                // instance field access may trigger NPE
                rvalue instanceof FieldAccess ||
                // array access may trigger NPE
                rvalue instanceof ArrayAccess) {
            return false;
        }
        if (rvalue instanceof ArithmeticExp) {
            ArithmeticExp.Op op = ((ArithmeticExp) rvalue).getOperator();
            // may trigger DivideByZeroException
            return op != ArithmeticExp.Op.DIV && op != ArithmeticExp.Op.REM;
        }
        return true;
    }
}
