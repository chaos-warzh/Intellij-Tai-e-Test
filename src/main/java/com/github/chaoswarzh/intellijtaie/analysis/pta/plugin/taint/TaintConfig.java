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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.util.InvokeUtils;
import com.github.chaoswarzh.intellijtaie.config.ConfigException;
import com.github.chaoswarzh.intellijtaie.language.classes.ClassHierarchy;
import com.github.chaoswarzh.intellijtaie.language.classes.JClass;
import com.github.chaoswarzh.intellijtaie.language.classes.JField;
import com.github.chaoswarzh.intellijtaie.language.classes.JMethod;
import com.github.chaoswarzh.intellijtaie.language.classes.SignatureMatcher;
import com.github.chaoswarzh.intellijtaie.language.type.ArrayType;
import com.github.chaoswarzh.intellijtaie.language.type.ClassType;
import com.github.chaoswarzh.intellijtaie.language.type.Type;
import com.github.chaoswarzh.intellijtaie.language.type.TypeSystem;
import com.github.chaoswarzh.intellijtaie.util.collection.Lists;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static com.github.chaoswarzh.intellijtaie.analysis.pta.plugin.taint.IndexRef.ARRAY_SUFFIX;

/**
 * Configuration for taint analysis.
 */
record TaintConfig(List<Source> sources,
                   List<Sink> sinks,
                   List<TaintTransfer> transfers,
                   List<ParamSanitizer> paramSanitizers,
                   boolean callSiteMode) {

    private static final Logger logger = LogManager.getLogger(TaintConfig.class);

    /**
     * An empty taint config.
     */
    private static final TaintConfig EMPTY = new TaintConfig(
            List.of(), List.of(), List.of(), List.of(), false);

    /**
     * Loads a taint analysis configuration from given path.
     * If the path is a file, then loads config from the file;
     * if the path is a directory, then loads all YAML files in the directory
     * and merge them as the result.
     *
     * @param path       the path
     * @param hierarchy  the class hierarchy
     * @param typeSystem the type manager
     * @return the resulting {@link TaintConfig}
     * @throws ConfigException if failed to load the config
     */
    static TaintConfig loadConfig(
            String path, ClassHierarchy hierarchy, TypeSystem typeSystem) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        SimpleModule module = new SimpleModule();
        module.addDeserializer(TaintConfig.class,
                new Deserializer(hierarchy, typeSystem));
        mapper.registerModule(module);
        File file = new File(path);
        logger.info("Loading taint config from {}", file.getAbsolutePath());
        if (file.isFile()) {
            return loadSingle(mapper, file);
        } else if (file.isDirectory()) {
            // if file is a directory, then load all YAML files
            // in the directory and merge them as the result
            TaintConfig[] result = new TaintConfig[]{ EMPTY };
            try (Stream<Path> paths = Files.walk(file.toPath())) {
                paths.filter(TaintConfig::isYAML)
                        .map(p -> loadSingle(mapper, p.toFile()))
                        .forEach(tc -> result[0] = result[0].mergeWith(tc));
                return result[0];
            } catch (IOException e) {
                throw new ConfigException("Failed to load taint config from " + file, e);
            }
        } else {
            throw new ConfigException(path + " is neither a file nor a directory");
        }
    }

    /**
     * Loads taint config from a single file.
     */
    private static TaintConfig loadSingle(ObjectMapper mapper, File file) {
        try {
            return mapper.readValue(file, TaintConfig.class);
        } catch (IOException e) {
            throw new ConfigException("Failed to load taint config from " + file, e);
        }
    }

    private static boolean isYAML(Path path) {
        String pathStr = path.toString();
        return pathStr.endsWith(".yml") || pathStr.endsWith(".yaml");
    }

    /**
     * Merges this taint config with other taint config.
     * @return a new merged taint config.
     */
    TaintConfig mergeWith(TaintConfig other) {
        return new TaintConfig(
                Lists.concatDistinct(sources, other.sources),
                Lists.concatDistinct(sinks, other.sinks),
                Lists.concatDistinct(transfers, other.transfers),
                Lists.concatDistinct(paramSanitizers, other.paramSanitizers),
                callSiteMode || other.callSiteMode);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TaintConfig:");
        if (!sources.isEmpty()) {
            sb.append("\nsources:\n");
            sources.forEach(source ->
                    sb.append("  ").append(source).append("\n"));
        }
        if (!sinks.isEmpty()) {
            sb.append("\nsinks:\n");
            sinks.forEach(sink ->
                    sb.append("  ").append(sink).append("\n"));
        }
        if (!transfers.isEmpty()) {
            sb.append("\ntransfers:\n");
            transfers.forEach(transfer ->
                    sb.append("  ").append(transfer).append("\n"));
        }
        if (!paramSanitizers.isEmpty()) {
            sb.append("\nsanitizers:\n");
            paramSanitizers.forEach(sanitizer ->
                    sb.append("  ").append(sanitizer).append("\n"));
        }
        return sb.toString();
    }

    /**
     * Deserializer for {@link TaintConfig}.
     */
    private static class Deserializer extends JsonDeserializer<TaintConfig> {

        private final SignatureMatcher matcher;

        private final TypeSystem typeSystem;

        private Deserializer(ClassHierarchy hierarchy, TypeSystem typeSystem) {
            this.matcher = new SignatureMatcher(hierarchy);
            this.typeSystem = typeSystem;
        }

        @Override
        public TaintConfig deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
            ObjectCodec oc = p.getCodec();
            JsonNode node = oc.readTree(p);
            List<Source> sources = deserializeSources(node.get("sources"));
            List<Sink> sinks = deserializeSinks(node.get("sinks"));
            List<TaintTransfer> transfers = deserializeTransfers(node.get("transfers"));
            List<ParamSanitizer> sanitizers = deserializeSanitizers(node.get("sanitizers"));
            JsonNode callSiteNode = node.get("call-site-mode");
            boolean callSiteMode = (callSiteNode != null && callSiteNode.asBoolean());
            return new TaintConfig(
                    sources, sinks, transfers, sanitizers, callSiteMode);
        }

        /**
         * Deserializes a {@link JsonNode} (assume it is an {@link ArrayNode})
         * to a list of {@link Source}.
         *
         * @param node the node to be deserialized
         * @return list of deserialized {@link Source}
         */
        private List<Source> deserializeSources(JsonNode node) {
            if (node instanceof ArrayNode arrayNode) {
                List<Source> result = new ArrayList<>();
                for (JsonNode elem : arrayNode) {
                    JsonNode sourceKind = elem.get("kind");
                    List<? extends Source> sources;
                    if (sourceKind != null) {
                        sources = switch (sourceKind.asText()) {
                            case "call" -> deserializeCallSources(elem);
                            case "param" -> deserializeParamSources(elem);
                            case "field" -> deserializeFieldSources(elem);
                            default -> {
                                logger.warn("Unknown source kind \"{}\" in {}",
                                        sourceKind.asText(), elem.toString());
                                yield List.of();
                            }
                        };
                    } else {
                        logger.warn("Ignore {} due to missing source \"kind\"",
                                elem.toString());
                        sources = List.of();
                    }
                    result.addAll(sources);
                }
                return Collections.unmodifiableList(result);
            } else {
                // if node is not an instance of ArrayNode, just return an empty set.
                return List.of();
            }
        }

        private List<CallSource> deserializeCallSources(JsonNode node) {
            String rawEntry = node.toString();
            String methodSig = node.get("method").asText();
            List<CallSource> result = matcher.getMethods(methodSig).stream().map(method -> {
                IndexRef indexRef = toIndexRef(method, node.get("index").asText());
                JsonNode typeNode = node.get("type");
                Type type = (typeNode != null)
                        ? typeSystem.getType(typeNode.asText())
                        // type not given, retrieve it from method signature
                        : getMethodType(method, indexRef.index());
                return new CallSource(method, indexRef, type, rawEntry);
            }).toList();
            if (result.isEmpty()) {
                // if we do not find matched methods with the signature
                // given in config file, just ignore it.
                logger.warn("Cannot find source method '{}'", methodSig);
            }
            return result;
        }

        private List<ParamSource> deserializeParamSources(JsonNode node) {
            String rawEntry = node.toString();
            String methodSig = node.get("method").asText();
            List<ParamSource> result = matcher.getMethods(methodSig).stream().map(method -> {
                IndexRef indexRef = toIndexRef(method, node.get("index").asText());
                JsonNode typeNode = node.get("type");
                Type type = (typeNode != null)
                        ? typeSystem.getType(typeNode.asText())
                        // type not given, retrieve it from method signature
                        : getMethodType(method, indexRef.index());
                return new ParamSource(method, indexRef, type, rawEntry);
            }).toList();
            if (result.isEmpty()) {
                // if we do not find matched methods with the signature
                // given in config file, just ignore it.
                logger.warn("Cannot find source method '{}'", methodSig);
            }
            return result;
        }

        private List<FieldSource> deserializeFieldSources(JsonNode node) {
            String rawEntry = node.toString();
            String fieldSig = node.get("field").asText();
            List<FieldSource> result = matcher.getFields(fieldSig).stream().map(field -> {
                JsonNode typeNode = node.get("type");
                Type type = (typeNode != null)
                        ? typeSystem.getType(typeNode.asText())
                        : field.getType(); // type not given, use field type
                return new FieldSource(field, type, rawEntry);
            }).toList();
            if (result.isEmpty()) {
                // if we do not find matched fields with the signature
                // given in config file, just ignore it.
                logger.warn("Cannot find source field '{}'", fieldSig);
            }
            return result;
        }

        /**
         * @return corresponding type of index for the method.
         */
        private static Type getMethodType(JMethod method, int index) {
            return switch (index) {
                case InvokeUtils.BASE -> method.getDeclaringClass().getType();
                case InvokeUtils.RESULT -> method.getReturnType();
                default -> method.getParamType(index);
            };
        }

        /**
         * Deserializes a {@link JsonNode} (assume it is an {@link ArrayNode})
         * to a list of {@link Sink}.
         *
         * @param node the node to be deserialized
         * @return list of deserialized {@link Sink}
         */
        private List<Sink> deserializeSinks(JsonNode node) {
            if (node instanceof ArrayNode arrayNode) {
                List<Sink> result = new ArrayList<>();
                for (JsonNode elem : arrayNode) {
                    String rawEntry = elem.toString();
                    String methodSig = elem.get("method").asText();
                    List<Sink> sinks = matcher.getMethods(methodSig).stream().map(method -> {
                        IndexRef indexRef = toIndexRef(method, elem.get("index").asText());
                        return new Sink(method, indexRef, rawEntry);
                    }).toList();
                    if (sinks.isEmpty()) {
                        // if we do not find matched methods with the signature
                        // given in config file, just ignore it.
                        logger.warn("Cannot find sink method '{}'", methodSig);
                    }
                    result.addAll(sinks);
                }
                return Collections.unmodifiableList(result);
            } else {
                // if node is not an instance of ArrayNode, just return an empty set.
                return List.of();
            }
        }

        /**
         * Deserializes a {@link JsonNode} (assume it is an {@link ArrayNode})
         * to a list of {@link TaintTransfer}.
         *
         * @param node the node to be deserialized
         * @return list of deserialized {@link TaintTransfer}
         */
        private List<TaintTransfer> deserializeTransfers(JsonNode node) {
            if (node instanceof ArrayNode arrayNode) {
                List<TaintTransfer> result = new ArrayList<>();
                for (JsonNode elem : arrayNode) {
                    String rawEntry = elem.toString();
                    String methodSig = elem.get("method").asText();
                    List<TaintTransfer> transfers = matcher.getMethods(methodSig).stream().map(method -> {
                        IndexRef from = toIndexRef(method, elem.get("from").asText());
                        IndexRef to = toIndexRef(method, elem.get("to").asText());
                        JsonNode typeNode = elem.get("type");
                        Type type;
                        if (typeNode != null) {
                            type = typeSystem.getType(typeNode.asText());
                        } else {
                            // type not given, retrieve it from method signature
                            Type varType = getMethodType(method, to.index());
                            type = switch (to.kind()) {
                                case VAR -> varType;
                                case ARRAY -> ((ArrayType) varType).elementType();
                                case FIELD -> to.field().getType();
                            };
                        }
                        return new TaintTransfer(method, from, to, type, rawEntry);
                    }).toList();
                    if (transfers.isEmpty()) {
                        // if we do not find matched methods with the signature
                        // given in config file, just ignore it.
                        logger.warn("Cannot find taint-transfer method '{}'", methodSig);
                    }
                    result.addAll(transfers);
                }
                return Collections.unmodifiableList(result);
            } else {
                // if node is not an instance of ArrayNode, just return an empty set.
                return List.of();
            }
        }

        private IndexRef toIndexRef(JMethod method, String text) {
            IndexRef.Kind kind;
            String indexStr;
            if (text.endsWith(ARRAY_SUFFIX)) {
                kind = IndexRef.Kind.ARRAY;
                indexStr = text.substring(0, text.length() - ARRAY_SUFFIX.length());
            } else if (text.contains(".")) {
                kind = IndexRef.Kind.FIELD;
                indexStr = text.substring(0, text.indexOf('.'));
            } else {
                kind = IndexRef.Kind.VAR;
                indexStr = text;
            }
            int index = InvokeUtils.toInt(indexStr);
            Type varType = getMethodType(method, index);
            JField field = null;
            switch (kind) {
                case ARRAY -> {
                    if (!(varType instanceof ArrayType)) {
                        throw new ConfigException(
                                "Expected: array type, given: " + varType);
                    }
                }
                case FIELD -> {
                    String fieldName = text.substring(text.indexOf('.') + 1);
                    if (varType instanceof ClassType classType) {
                        JClass clazz = classType.getJClass();
                        while (clazz != null) {
                            field = clazz.getDeclaredField(fieldName);
                            if (field != null) {
                                break;
                            }
                            clazz = clazz.getSuperClass();
                        }
                    }
                    if (field == null) {
                        throw new ConfigException("Cannot find field '"
                                + fieldName + "' in type " + varType);
                    }
                }
            }
            return new IndexRef(kind, index, field);
        }

        /**
         * Deserializes a {@link JsonNode} (assume it is an {@link ArrayNode})
         * to a list of {@link Sanitizer}.
         *
         * @param node the node to be deserialized
         * @return list of deserialized {@link Sanitizer}.
         */
        private List<ParamSanitizer> deserializeSanitizers(JsonNode node) {
            if (node instanceof ArrayNode arrayNode) {
                List<ParamSanitizer> result = new ArrayList<>();
                for (JsonNode elem : arrayNode) {
                    String methodSig = elem.get("method").asText();
                    List<ParamSanitizer> sanitizers = matcher.getMethods(methodSig).stream().map(method -> {
                        int index = InvokeUtils.toInt(elem.get("index").asText());
                        return new ParamSanitizer(method, index);
                    }).toList();
                    if (sanitizers.isEmpty()) {
                        logger.warn("Cannot find sanitizer method '{}'", methodSig);
                    }
                    result.addAll(sanitizers);
                }
                return Collections.unmodifiableList(result);
            } else {
                // if node is not an instance of ArrayNode, just return an empty set.
                return List.of();
            }
        }
    }
}
