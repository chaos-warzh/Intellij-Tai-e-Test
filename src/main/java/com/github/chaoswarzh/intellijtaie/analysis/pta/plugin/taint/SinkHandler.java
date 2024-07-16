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

import com.github.chaoswarzh.intellijtaie.analysis.graph.callgraph.CallKind;
import com.github.chaoswarzh.intellijtaie.analysis.graph.callgraph.Edge;
import com.github.chaoswarzh.intellijtaie.analysis.pta.PointerAnalysisResult;
import com.github.chaoswarzh.intellijtaie.analysis.pta.core.cs.element.ArrayIndex;
import com.github.chaoswarzh.intellijtaie.analysis.pta.core.cs.element.CSObj;
import com.github.chaoswarzh.intellijtaie.analysis.pta.core.cs.element.InstanceField;
import com.github.chaoswarzh.intellijtaie.analysis.pta.core.cs.element.Pointer;
import com.github.chaoswarzh.intellijtaie.analysis.pta.core.heap.Obj;
import com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.util.InvokeUtils;
import com.github.chaoswarzh.intellijtaie.ir.exp.Var;
import com.github.chaoswarzh.intellijtaie.ir.stmt.Invoke;
import com.github.chaoswarzh.intellijtaie.language.classes.JMethod;
import com.github.chaoswarzh.intellijtaie.util.collection.MultiMap;
import com.github.chaoswarzh.intellijtaie.util.collection.MultiMapCollector;
import com.github.chaoswarzh.intellijtaie.util.collection.Sets;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles sinks in taint analysis.
 */
class SinkHandler extends Handler {

    private final List<Sink> sinks;

    SinkHandler(HandlerContext context) {
        super(context);
        sinks = context.config().sinks();
    }

    Set<TaintFlow> collectTaintFlows() {
        PointerAnalysisResult result = solver.getResult();
        Set<TaintFlow> taintFlows = Sets.newOrderedSet();
        for (Sink sink : sinks) {
            result.getCallGraph()
                    .edgesInTo(sink.method())
                    // TODO: handle other call edges
                    .filter(e -> e.getKind() != CallKind.OTHER)
                    .map(Edge::getCallSite)
                    .map(sinkCall -> collectTaintFlows(sinkCall, sink))
                    .forEach(taintFlows::addAll);
        }
        if (callSiteMode) {
            MultiMap<JMethod, Sink> sinkMap = sinks.stream()
                    .collect(MultiMapCollector.get(Sink::method, s -> s));
            // scan all reachable call sites to search sink calls
            result.getCallGraph()
                    .reachableMethods()
                    .flatMap(m -> m.getIR().invokes(false))
                    .forEach(callSite -> {
                        JMethod callee = callSite.getMethodRef().resolveNullable();
                        if (callee != null) {
                            for (Sink sink : sinkMap.get(callee)) {
                                taintFlows.addAll(collectTaintFlows(callSite, sink));
                            }
                        }
                    });
        }
        return taintFlows;
    }

    private Set<TaintFlow> collectTaintFlows(
            Invoke sinkCall, Sink sink) {
        IndexRef indexRef = sink.indexRef();
        Var arg = InvokeUtils.getVar(sinkCall, indexRef.index());
        SinkPoint sinkPoint = new SinkPoint(sinkCall, indexRef, sink);
        // obtain objects to check for different IndexRef.Kind
        Set<Obj> objs = switch (indexRef.kind()) {
            case VAR -> csManager.getCSVarsOf(arg)
                    .stream()
                    .flatMap(Pointer::objects)
                    .map(CSObj::getObject)
                    .collect(Collectors.toUnmodifiableSet());
            case ARRAY -> csManager.getCSVarsOf(arg)
                    .stream()
                    .flatMap(Pointer::objects)
                    .map(csManager::getArrayIndex)
                    .flatMap(ArrayIndex::objects)
                    .map(CSObj::getObject)
                    .collect(Collectors.toUnmodifiableSet());
            case FIELD -> csManager.getCSVarsOf(arg)
                    .stream()
                    .flatMap(Pointer::objects)
                    .map(o -> csManager.getInstanceField(o, indexRef.field()))
                    .flatMap(InstanceField::objects)
                    .map(CSObj::getObject)
                    .collect(Collectors.toUnmodifiableSet());
        };
        return objs.stream()
                .filter(manager::isTaint)
                .map(manager::getSourcePoint)
                .map(sourcePoint -> new TaintFlow(sourcePoint, sinkPoint))
                .collect(Collectors.toSet());
    }
}
