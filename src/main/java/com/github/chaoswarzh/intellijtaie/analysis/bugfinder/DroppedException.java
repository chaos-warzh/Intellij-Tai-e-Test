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

package com.github.chaoswarzh.intellijtaie.analysis.bugfinder;

import com.github.chaoswarzh.intellijtaie.analysis.MethodAnalysis;
import com.github.chaoswarzh.intellijtaie.config.AnalysisConfig;
import com.github.chaoswarzh.intellijtaie.ir.IR;
import com.github.chaoswarzh.intellijtaie.ir.proginfo.ExceptionEntry;
import com.github.chaoswarzh.intellijtaie.ir.stmt.Goto;
import com.github.chaoswarzh.intellijtaie.ir.stmt.Nop;
import com.github.chaoswarzh.intellijtaie.ir.stmt.Return;
import com.github.chaoswarzh.intellijtaie.ir.stmt.Stmt;
import com.github.chaoswarzh.intellijtaie.language.classes.ClassNames;
import com.github.chaoswarzh.intellijtaie.util.collection.Sets;

import java.util.Set;

public class DroppedException extends MethodAnalysis<Set<BugInstance>> {

    public static final String ID = "dropped-exception";

    public DroppedException(AnalysisConfig config) {
        super(config);
    }

    @Override
    public Set<BugInstance> analyze(IR ir) {
        Set<BugInstance> bugInstanceSet = Sets.newHybridSet();
        for (ExceptionEntry entry : ir.getExceptionEntries()) {
            String exceptionName = entry.catchType().getName();
            if (exceptionName.equals(ClassNames.CLONE_NOT_SUPPORTED_EXCEPTION)
                    || exceptionName.equals(ClassNames.INTERRUPTED_EXCEPTION)) {
                continue;
            }
            Stmt catchHandler = entry.handler();
            int nextStmt = catchHandler.getIndex() + 1;

            while (nextStmt < ir.getStmts().size() && ir.getStmt(nextStmt) instanceof Nop) {
                nextStmt++;
            }

            if (nextStmt < ir.getStmts().size() &&
                    (ir.getStmt(nextStmt) instanceof Goto || ir.getStmt(nextStmt) instanceof Return)) {
                boolean exitInTryBlock = false;
                for (int i = entry.start().getIndex(); i <= entry.end().getIndex(); ++i) {
                    if (ir.getStmt(i) instanceof Return) {
                        exitInTryBlock = true;
                        break;
                    }
                }
                Severity severity = Severity.MINOR;
                if (exceptionName.equals(ClassNames.ERROR)
                        || exceptionName.equals(ClassNames.EXCEPTION)
                        || exceptionName.equals(ClassNames.THROWABLE)
                        || exceptionName.equals(ClassNames.RUNTIME_EXCEPTION)) {
                    severity = Severity.CRITICAL;
                }
                BugInstance bugInstance = new BugInstance(
                        exitInTryBlock ? BugType.DE_MIGHT_DROP : BugType.DE_MIGHT_IGNORE,
                        severity, ir.getMethod())
                        .setSourceLine(catchHandler.getLineNumber());
                bugInstanceSet.add(bugInstance);
            }
        }

        return bugInstanceSet;
    }

    private enum BugType implements com.github.chaoswarzh.intellijtaie.analysis.bugfinder.BugType {
        DE_MIGHT_DROP,
        DE_MIGHT_IGNORE
    }
}
