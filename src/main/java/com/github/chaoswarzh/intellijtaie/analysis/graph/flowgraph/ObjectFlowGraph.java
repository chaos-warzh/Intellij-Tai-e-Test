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

package com.github.chaoswarzh.intellijtaie.analysis.graph.flowgraph;

import com.github.chaoswarzh.intellijtaie.analysis.graph.callgraph.CallGraph;
import com.github.chaoswarzh.intellijtaie.analysis.graph.callgraph.CallKind;
import com.github.chaoswarzh.intellijtaie.analysis.pta.core.cs.element.ArrayIndex;
import com.github.chaoswarzh.intellijtaie.analysis.pta.core.cs.element.CSVar;
import com.github.chaoswarzh.intellijtaie.analysis.pta.core.cs.element.InstanceField;
import com.github.chaoswarzh.intellijtaie.analysis.pta.core.cs.element.Pointer;
import com.github.chaoswarzh.intellijtaie.analysis.pta.core.cs.element.StaticField;
import com.github.chaoswarzh.intellijtaie.analysis.pta.core.solver.PointerFlowEdge;
import com.github.chaoswarzh.intellijtaie.analysis.pta.core.solver.PointerFlowGraph;
import com.github.chaoswarzh.intellijtaie.ir.exp.InvokeInstanceExp;
import com.github.chaoswarzh.intellijtaie.ir.exp.Var;
import com.github.chaoswarzh.intellijtaie.ir.stmt.Invoke;
import com.github.chaoswarzh.intellijtaie.language.classes.JMethod;
import com.github.chaoswarzh.intellijtaie.util.Indexer;
import com.github.chaoswarzh.intellijtaie.util.collection.IndexMap;
import com.github.chaoswarzh.intellijtaie.util.collection.Maps;
import com.github.chaoswarzh.intellijtaie.util.collection.MultiMap;
import com.github.chaoswarzh.intellijtaie.util.collection.Views;
import com.github.chaoswarzh.intellijtaie.util.graph.Graph;

import java.util.Set;

public class ObjectFlowGraph extends NodeManager
        implements Graph<Node>, Indexer<Node> {

    private final MultiMap<Node, FlowEdge> inEdges = Maps.newMultiMap(
            new IndexMap<>(this, 4096));

    private final MultiMap<Node, FlowEdge> outEdges = Maps.newMultiMap(
            new IndexMap<>(this, 4096));

    public ObjectFlowGraph(PointerFlowGraph pfg,
                           CallGraph<Invoke, JMethod> callGraph) {
        pfg.pointers().forEach(pointer -> {
            toNode(pointer); // ensure every pointer has a corresponding node
            pfg.getOutEdgesOf(pointer).forEach(this::addPointerFlowEdge);
        });
        // This-passing edges are absent on PFG, so we iterate call graph edges
        // to complement this kind of edges.
        callGraph.edges()
                .forEach(e -> {
                    // Currently ignore OTHER (e.g., reflective) call edges
                    if (e.getKind() != CallKind.OTHER &&
                            e.getCallSite().getInvokeExp() instanceof
                                    InvokeInstanceExp invokeExp) {
                        Var base = invokeExp.getBase();
                        Var thisVar = e.getCallee().getIR().getThis();
                        addFlowEdge(new BasicFlowEdge(
                                FlowKind.THIS_PASSING,
                                getOrCreateVarNode(base),
                                getOrCreateVarNode(thisVar)));
                    }
                });
    }

    private void addPointerFlowEdge(PointerFlowEdge edge) {
        FlowKind kind = edge.kind();
        Node source = toNode(edge.source());
        Node target = toNode(edge.target());
        FlowEdge flowEdge = switch (kind) {
            case OTHER -> new OtherFlowEdge(edge.getInfo(), source, target, edge);
            default -> new BasicFlowEdge(kind, source, target);
        };
        addFlowEdge(flowEdge);
    }

    private void addFlowEdge(FlowEdge flowEdge) {
        outEdges.put(flowEdge.source(), flowEdge);
        inEdges.put(flowEdge.target(), flowEdge);
    }

    /**
     * Converts given pointer to a node in this OFG.
     */
    private Node toNode(Pointer pointer) {
        if (pointer instanceof CSVar csVar) {
            return getOrCreateVarNode(csVar.getVar());
        } else if (pointer instanceof InstanceField iField) {
            return getOrCreateInstanceFieldNode(
                    iField.getBase().getObject(), iField.getField());
        } else if (pointer instanceof ArrayIndex arrayIndex) {
            return getOrCreateArrayIndexNode(
                    arrayIndex.getArray().getObject());
        } else {
            return getOrCreateStaticFieldNode(
                    ((StaticField) pointer).getField());
        }
    }

    @Override
    public Set<Node> getPredsOf(Node node) {
        return Views.toMappedSet(getInEdgesOf(node), FlowEdge::source);
    }

    @Override
    public Set<FlowEdge> getInEdgesOf(Node node) {
        return inEdges.get(node);
    }

    @Override
    public Set<Node> getSuccsOf(Node node) {
        return Views.toMappedSet(getOutEdgesOf(node), FlowEdge::target);
    }

    @Override
    public Set<FlowEdge> getOutEdgesOf(Node node) {
        return outEdges.get(node);
    }
}
