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

package com.github.chaoswarzh.intellijtaie.analysis.pta;

import org.apache.logging.log4j.Level;
import com.github.chaoswarzh.intellijtaie.World;
import com.github.chaoswarzh.intellijtaie.analysis.ProgramAnalysis;
import com.github.chaoswarzh.intellijtaie.analysis.pta.core.cs.element.MapBasedCSManager;
import com.github.chaoswarzh.intellijtaie.analysis.pta.core.cs.selector.ContextSelector;
import com.github.chaoswarzh.intellijtaie.analysis.pta.core.cs.selector.ContextSelectorFactory;
import com.github.chaoswarzh.intellijtaie.analysis.pta.core.heap.AllocationSiteBasedModel;
import com.github.chaoswarzh.intellijtaie.analysis.pta.core.heap.HeapModel;
import com.github.chaoswarzh.intellijtaie.analysis.pta.core.solver.DefaultSolver;
import com.github.chaoswarzh.intellijtaie.analysis.pta.core.solver.Solver;
import com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.AnalysisTimer;
import com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.ClassInitializer;
import com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.CompositePlugin;
import com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.EntryPointHandler;
import com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.Plugin;
import com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.ReferenceHandler;
import com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.ResultProcessor;
import com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.ThreadHandler;
import com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.exception.ExceptionAnalysis;
import com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.invokedynamic.InvokeDynamicAnalysis;
import com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.invokedynamic.Java9StringConcatHandler;
import com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.invokedynamic.LambdaAnalysis;
import com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.natives.NativeModeller;
import com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.reflection.ReflectionAnalysis;
import com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.taint.TaintAnalysis;
import com.github.chaoswarzh.intellijtaie.analysis.pta.toolkit.CollectionMethods;
import com.github.chaoswarzh.intellijtaie.analysis.pta.toolkit.mahjong.Mahjong;
import com.github.chaoswarzh.intellijtaie.analysis.pta.toolkit.scaler.Scaler;
import com.github.chaoswarzh.intellijtaie.analysis.pta.toolkit.zipper.Zipper;
import com.github.chaoswarzh.intellijtaie.config.AnalysisConfig;
import com.github.chaoswarzh.intellijtaie.config.AnalysisOptions;
import com.github.chaoswarzh.intellijtaie.config.ConfigException;
import com.github.chaoswarzh.intellijtaie.util.AnalysisException;
import com.github.chaoswarzh.intellijtaie.util.Timer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class PointerAnalysis extends ProgramAnalysis<PointerAnalysisResult> {

    public static final String ID = "pta";

    public PointerAnalysis(AnalysisConfig config) {
        super(config);
    }

    @Override
    public PointerAnalysisResult analyze() {
        AnalysisOptions options = getOptions();
        HeapModel heapModel = new AllocationSiteBasedModel(options);
        ContextSelector selector = null;
        String advanced = options.getString("advanced");
        String cs = options.getString("cs");
        if (advanced != null) {
            if (advanced.equals("collection")) {
                selector = ContextSelectorFactory.makeSelectiveSelector(cs,
                        new CollectionMethods(World.get().getClassHierarchy()).get());
            } else {
                // run context-insensitive analysis as pre-analysis
                PointerAnalysisResult preResult = runAnalysis(heapModel,
                        ContextSelectorFactory.makeCISelector());
                if (advanced.startsWith("scaler")) {
                    selector = Timer.runAndCount(() -> ContextSelectorFactory
                                    .makeGuidedSelector(Scaler.run(preResult, advanced)),
                            "Scaler", Level.INFO);
                } else if (advanced.startsWith("zipper")) {
                    selector = Timer.runAndCount(() -> ContextSelectorFactory
                                    .makeSelectiveSelector(cs, Zipper.run(preResult, advanced)),
                            "Zipper", Level.INFO);
                } else if (advanced.equals("mahjong")) {
                    heapModel = Timer.runAndCount(() -> Mahjong.run(preResult, options),
                            "Mahjong", Level.INFO);
                } else {
                    throw new IllegalArgumentException(
                            "Illegal advanced analysis argument: " + advanced);
                }
            }
        }
        if (selector == null) {
            selector = ContextSelectorFactory.makePlainSelector(cs);
        }
        return runAnalysis(heapModel, selector);
    }

    private PointerAnalysisResult runAnalysis(HeapModel heapModel,
                                              ContextSelector selector) {
        AnalysisOptions options = getOptions();
        Solver solver = new DefaultSolver(options,
                heapModel, selector, new MapBasedCSManager());
        // The initialization of some Plugins may read the fields in solver,
        // e.g., contextSelector or csManager, thus we initialize Plugins
        // after setting all other fields of solver.
        setPlugin(solver, options);
        solver.solve();
        return solver.getResult();
    }

    private static void setPlugin(Solver solver, AnalysisOptions options) {
        CompositePlugin plugin = new CompositePlugin();
        // add builtin plugins
        // To record elapsed time precisely, AnalysisTimer should be added at first.
        plugin.addPlugin(
                new AnalysisTimer(),
                new EntryPointHandler(),
                new ClassInitializer(),
                new ThreadHandler(),
                new NativeModeller(),
                new ExceptionAnalysis()
        );
        int javaVersion = World.get().getOptions().getJavaVersion();
        if (javaVersion < 9) {
            // current reference handler doesn't support Java 9+
            plugin.addPlugin(new ReferenceHandler());
        }
        if (javaVersion >= 8) {
            plugin.addPlugin(new LambdaAnalysis());
        }
        if (javaVersion >= 9) {
            plugin.addPlugin(new Java9StringConcatHandler());
        }
        if (options.getString("reflection-inference") != null ||
                options.getString("reflection-log") != null) {
            plugin.addPlugin(new ReflectionAnalysis());
        }
        if (options.getBoolean("handle-invokedynamic") &&
                InvokeDynamicAnalysis.useMethodHandle()) {
            plugin.addPlugin(new InvokeDynamicAnalysis());
        }
        if (options.getString("taint-config") != null) {
            plugin.addPlugin(new TaintAnalysis());
        }
        plugin.addPlugin(new ResultProcessor());
        // add plugins specified in options
        // noinspection unchecked
        addPlugins(plugin, (List<String>) options.get("plugins"));
        // connects plugins and solver
        plugin.setSolver(solver);
        solver.setPlugin(plugin);
    }

    private static void addPlugins(CompositePlugin plugin,
                                   List<String> pluginClasses) {
        for (String pluginClass : pluginClasses) {
            try {
                Class<?> clazz = Class.forName(pluginClass);
                Constructor<?> ctor = clazz.getConstructor();
                Plugin newPlugin = (Plugin) ctor.newInstance();
                plugin.addPlugin(newPlugin);
            } catch (ClassNotFoundException e) {
                throw new ConfigException(
                        "Plugin class " + pluginClass + " is not found");
            } catch (IllegalAccessException | NoSuchMethodException e) {
                throw new AnalysisException("Failed to get constructor of " +
                        pluginClass + ", does the plugin class" +
                        " provide a public non-arg constructor?");
            } catch (InvocationTargetException | InstantiationException e) {
                throw new AnalysisException(
                        "Failed to create plugin instance for " + pluginClass, e);
            }
        }
    }
}
