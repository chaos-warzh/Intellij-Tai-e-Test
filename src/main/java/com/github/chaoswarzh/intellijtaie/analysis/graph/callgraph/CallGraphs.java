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

package com.github.chaoswarzh.intellijtaie.analysis.graph.callgraph;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.github.chaoswarzh.intellijtaie.World;
import com.github.chaoswarzh.intellijtaie.ir.IRPrinter;
import com.github.chaoswarzh.intellijtaie.ir.exp.InvokeDynamic;
import com.github.chaoswarzh.intellijtaie.ir.exp.InvokeExp;
import com.github.chaoswarzh.intellijtaie.ir.exp.InvokeInterface;
import com.github.chaoswarzh.intellijtaie.ir.exp.InvokeSpecial;
import com.github.chaoswarzh.intellijtaie.ir.exp.InvokeStatic;
import com.github.chaoswarzh.intellijtaie.ir.exp.InvokeVirtual;
import com.github.chaoswarzh.intellijtaie.ir.proginfo.MethodRef;
import com.github.chaoswarzh.intellijtaie.ir.stmt.Invoke;
import com.github.chaoswarzh.intellijtaie.language.classes.JMethod;
import com.github.chaoswarzh.intellijtaie.language.type.Type;
import com.github.chaoswarzh.intellijtaie.util.AnalysisException;
import com.github.chaoswarzh.intellijtaie.util.Indexer;
import com.github.chaoswarzh.intellijtaie.util.SimpleIndexer;
import com.github.chaoswarzh.intellijtaie.util.collection.Maps;
import com.github.chaoswarzh.intellijtaie.util.graph.DotAttributes;
import com.github.chaoswarzh.intellijtaie.util.graph.DotDumper;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.Map;

/**
 * Static utility methods about call graph.
 */
public final class CallGraphs {

    private static final Logger logger = LogManager.getLogger(CallGraphs.class);

    private CallGraphs() {
    }

    public static CallKind getCallKind(Invoke invoke) {
        InvokeExp invokeExp = invoke.getInvokeExp();
        if (invokeExp instanceof InvokeVirtual) {
            return CallKind.VIRTUAL;
        } else if (invokeExp instanceof InvokeInterface) {
            return CallKind.INTERFACE;
        } else if (invokeExp instanceof InvokeSpecial) {
            return CallKind.SPECIAL;
        } else if (invokeExp instanceof InvokeStatic) {
            return CallKind.STATIC;
        } else if (invokeExp instanceof InvokeDynamic) {
            return CallKind.DYNAMIC;
        }
        throw new AnalysisException("Cannot handle Invoke: " + invoke);
    }

    @Nullable
    public static JMethod resolveCallee(Type type, Invoke callSite) {
        MethodRef methodRef = callSite.getMethodRef();
        if (callSite.isInterface() || callSite.isVirtual()) {
            return World.get().getClassHierarchy()
                    .dispatch(type, methodRef);
        } else if (callSite.isSpecial()) {
            return World.get().getClassHierarchy()
                    .dispatch(methodRef.getDeclaringClass(), methodRef);
        } else if (callSite.isStatic()) {
            return methodRef.resolveNullable();
        } else {
            throw new AnalysisException("Cannot resolve Invoke: " + callSite);
        }
    }

    /**
     * Dumps call graph to dot file.
     */
    static void dumpCallGraph(CallGraph<Invoke, JMethod> callGraph, File outFile) {
        logger.info("Dumping call graph to {}",
                outFile.getAbsolutePath());
        Indexer<JMethod> indexer = new SimpleIndexer<>();
        new DotDumper<JMethod>()
                .setNodeToString(n -> Integer.toString(indexer.getIndex(n)))
                .setNodeLabeler(JMethod::toString)
                .setGlobalNodeAttributes(DotAttributes.of("shape", "box",
                        "style", "filled", "color", "\".3 .2 1.0\""))
                .setEdgeLabeler(e -> IRPrinter.toString(
                        ((MethodEdge<Invoke, JMethod>) e).callSite()))
                .dump(callGraph, outFile);
    }

    static void dumpMethods(CallGraph<Invoke, JMethod> callGraph, File outFile) {
        try (PrintStream out =
                     new PrintStream(new FileOutputStream(outFile))) {
            logger.info("Dumping reachable methods to {}",
                    outFile.getAbsolutePath());
            callGraph.reachableMethods()
                    .map(JMethod::getSignature)
                    .sorted()
                    .forEach(out::println);
        } catch (FileNotFoundException e) {
            logger.warn("Failed to dump reachable methods to " + outFile, e);
        }
    }

    static void dumpCallEdges(CallGraph<Invoke, JMethod> callGraph, File outFile) {
        try (PrintStream out =
                     new PrintStream(new FileOutputStream(outFile))) {
            logger.info("Dumping call edges to {}",
                    outFile.getAbsolutePath());
            callGraph.reachableMethods()
                    // sort callers
                    .sorted(Comparator.comparing(JMethod::getSignature))
                    .forEach(m -> getInvokeReps(m).forEach((invoke, rep) ->
                            callGraph.getCalleesOf(invoke)
                                    .stream()
                                    .sorted(Comparator.comparing(JMethod::getSignature))
                                    .forEach(callee -> out.println(rep + "\t" + callee))));
        } catch (FileNotFoundException e) {
            logger.warn("Failed to dump call graph edges to "
                    + outFile.getAbsolutePath(), e);
        }
    }

    /**
     * @return a map from Invoke to its string representation in given method.
     */
    private static Map<Invoke, String> getInvokeReps(JMethod caller) {
        Map<String, Integer> counter = Maps.newMap();
        Map<Invoke, String> invokeReps =
                Maps.newOrderedMap(Comparator.comparing(Invoke::getIndex));
        caller.getIR().forEach(s -> {
            if (s instanceof Invoke invoke) {
                if (invoke.isDynamic()) { // skip invokedynamic
                    return;
                }
                MethodRef ref = invoke.getMethodRef();
                String target = ref.getDeclaringClass().getName() + "." + ref.getName();
                int n = getInvokeNumber(target, counter);
                String rep = caller + "/" + target + "/" + n;
                invokeReps.put(invoke, rep);
            }
        });
        return invokeReps;
    }

    private static int getInvokeNumber(String target, Map<String, Integer> counter) {
        Integer n = counter.get(target);
        if (n == null) {
            n = 0;
        }
        counter.put(target, n + 1);
        return n;
    }

    public static String toString(Invoke invoke) {
        return invoke.getContainer() + IRPrinter.toString(invoke);
    }
}
