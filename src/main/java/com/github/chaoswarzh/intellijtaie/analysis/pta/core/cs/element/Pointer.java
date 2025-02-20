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

package com.github.chaoswarzh.intellijtaie.analysis.pta.core.cs.element;

import com.github.chaoswarzh.intellijtaie.analysis.graph.flowgraph.FlowKind;
import com.github.chaoswarzh.intellijtaie.analysis.pta.core.solver.PointerFlowEdge;
import com.github.chaoswarzh.intellijtaie.analysis.pta.pts.PointsToSet;
import com.github.chaoswarzh.intellijtaie.language.type.Type;
import com.github.chaoswarzh.intellijtaie.util.Indexable;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Represents all pointers (nodes) in context-sensitive
 * pointer analysis (pointer flow graph).
 */
public interface Pointer extends Indexable {

    /**
     * Retrieves the points-to set associated with this pointer.
     * <p>
     * This method may return {@code null}.
     * We recommend use {@link #getObjects()} and {@link #objects()}
     * for accessing the objects pointed by this pointer after
     * the pointer analysis finishes.
     *
     * @return the points-to set associated with this pointer.
     */
    @Nullable
    PointsToSet getPointsToSet();

    /**
     * Sets the associated points-to set of this pointer.
     */
    void setPointsToSet(PointsToSet pointsToSet);

    /**
     * Adds filter to filter out objects pointed to by this pointer.
     */
    void addFilter(Predicate<CSObj> filter);

    /**
     * @return all filters added to this pointer.
     */
    Set<Predicate<CSObj>> getFilters();

    /**
     * Safely retrieves context-sensitive objects pointed to by this pointer.
     *
     * @return an empty set if {@code pointer} has not been associated
     * a {@code PointsToSet}; otherwise, returns set of objects in the
     * {@code PointsToSet}.
     */
    Set<CSObj> getObjects();

    /**
     * Safely retrieves context-sensitive objects pointed to by this pointer.
     *
     * @return an empty stream if {@code pointer} has not been associated
     * a {@code PointsToSet}; otherwise, returns stream of objects in the
     * {@code PointsToSet}.
     */
    Stream<CSObj> objects();

    /**
     * Adds a pointer flow edge and returns the edge in the PFG.
     * If the edge to add already exists, then
     * <ul>
     *     <li>if the edge is of {@link FlowKind#OTHER},
     *     returns the existing edge;
     *     <li>otherwise, returns {@code null}, meaning that the edge
     *     does not need to be processed again.
     * </ul>
     */
    PointerFlowEdge addEdge(PointerFlowEdge edge);

    /**
     * Removes out edges of this pointer if they satisfy the filter.
     * <p>
     * <strong>Note:</strong> This method should not be called outside of
     * {@link com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.Plugin#onPhaseFinish()},
     * otherwise it may break the monotonicity of pointer analysis.
     * </p>
     */
    void removeEdgesIf(Predicate<PointerFlowEdge> filter);

    /**
     * @return out edges of this pointer in pointer flow graph.
     */
    Set<PointerFlowEdge> getOutEdges();

    /**
     * @return out degree of this pointer in pointer flow graph.
     */
    int getOutDegree();

    /**
     * @return the type of this pointer
     */
    Type getType();
}
