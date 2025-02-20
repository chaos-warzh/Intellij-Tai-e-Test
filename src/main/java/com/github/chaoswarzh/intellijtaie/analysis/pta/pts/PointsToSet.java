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

package com.github.chaoswarzh.intellijtaie.analysis.pta.pts;

import com.github.chaoswarzh.intellijtaie.analysis.pta.core.cs.element.CSObj;
import com.github.chaoswarzh.intellijtaie.util.Copyable;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Representation of points-to sets that consist of {@link CSObj}.
 */
public interface PointsToSet extends Iterable<CSObj>, Copyable<PointsToSet> {

    /**
     * Adds an object to this set.
     *
     * @return true if this points-to set changed as a result of the call,
     * otherwise false.
     */
    boolean addObject(CSObj obj);

    /**
     * Adds all objects in given pts to this set.
     *
     * @return true if this points-to set changed as a result of the call,
     * otherwise false.
     */
    boolean addAll(PointsToSet pts);

    /**
     * Adds all objects in given pts to this set.
     *
     * @return the difference between {@code pts} and this set.
     */
    PointsToSet addAllDiff(PointsToSet pts);

    /**
     * Removes objects from this set if they satisfy the filter.
     * <p>
     * <strong>Note:</strong> This method should not be called outside of
     * {@link com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.Plugin#onPhaseFinish()},
     * otherwise it may break the monotonicity of pointer analysis.
     * </p>
     */
    void removeIf(Predicate<CSObj> filter);

    /**
     * @return true if this set contains given object, otherwise false.
     */
    boolean contains(CSObj obj);

    /**
     * @return whether this set if empty.
     */
    boolean isEmpty();

    /**
     * @return the number of objects in this set.
     */
    int size();

    /**
     * @return all objects in this set.
     */
    Set<CSObj> getObjects();

    /**
     * @return all objects in this set.
     */
    Stream<CSObj> objects();

    @Override
    default Iterator<CSObj> iterator() {
        return getObjects().iterator();
    }
}
