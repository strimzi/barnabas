/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.controller.cluster.resources;

import io.fabric8.kubernetes.api.model.HasMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableMap;

/**
 * An immutable set of labels
 */
public class Labels {

    public static final String STRIMZI_DOMAIN = "strimzi.io/";

    /**
     * The kind of a ConfigMap:
     * <ul>
     *     <li>{@code strimzi.io/kind=cluster}
     *         identifies a ConfigMap that is intended to be consumed by
     *         the cluster controller.</li>
     *     <li>{@code strimzi.io/kind=topic}
     *         identifies a ConfigMap that is intended to be consumed
     *         by the topic controller.</li>
     * </ul>
     */
    public static final String STRIMZI_KIND_LABEL = STRIMZI_DOMAIN + "kind";
    /**
     * The type of Strimzi component:
     * E.g: {@link io.strimzi.controller.cluster.operator.assembly.KafkaClusterOperations#CLUSTER_TYPE_KAFKA kafka},
     * {@link io.strimzi.controller.cluster.operator.assembly.KafkaClusterOperations#CLUSTER_TYPE_ZOOKEEPER zookeeper},
     * {@link io.strimzi.controller.cluster.operator.assembly.KafkaClusterOperations#CLUSTER_TYPE_TOPIC_CONTROLLER topic-controller},
     * {@link io.strimzi.controller.cluster.operator.assembly.KafkaConnectClusterOperations#CLUSTER_TYPE_CONNECT kafka-connect},
     * {@link io.strimzi.controller.cluster.operator.assembly.KafkaConnectS2IClusterOperations#CLUSTER_TYPE_CONNECT_S2I kafka-connect-s2i}
     */
    public static final String STRIMZI_TYPE_LABEL = STRIMZI_DOMAIN + "type";

    /**
     * The Strimzi cluster the resource is part of.
     * The value is the cluster name (i.e. the name of the cluster CM)
     */
    public static final String STRIMZI_CLUSTER_LABEL = STRIMZI_DOMAIN + "cluster";

    /**
     * The name of the K8S resource.
     * This is often the same name as the cluster
     * (i.e. the same as {@code strimzi.io/cluster})
     * but is different in some cases (e.g. headful and headless services)
     */
    public static final String STRIMZI_NAME_LABEL = STRIMZI_DOMAIN + "name";

    /**
     * The empty set of labels.
     */
    public static final Labels EMPTY = new Labels(emptyMap());

    private final Map<String, String> labels;

    /**
     * Returns the value of the {@code strimzi.io/cluster} label of the given {@code resource}.
     */
    public static String cluster(HasMetadata resource) {
        return resource.getMetadata().getLabels().get(Labels.STRIMZI_CLUSTER_LABEL);
    }

    /**
     * Returns the value of the {@code strimzi.io/type} label of the given {@code resource}.
     */
    public static String type(HasMetadata resource) {
        return resource.getMetadata().getLabels().get(Labels.STRIMZI_TYPE_LABEL);
    }

    /**
     * Returns the value of the {@code strimzi.io/name} label of the given {@code resource}.
     */
    public static String name(HasMetadata resource) {
        return resource.getMetadata().getLabels().get(Labels.STRIMZI_NAME_LABEL);
    }

    /**
     * Returns the value of the {@code strimzi.io/kind} label of the given {@code resource}.
     */
    public static String kind(HasMetadata resource) {
        return resource.getMetadata().getLabels().get(Labels.STRIMZI_KIND_LABEL);
    }

    public static Labels userLabels(Map<String, String> userLabels) {
        for (String key : userLabels.keySet()) {
            if (key.startsWith(STRIMZI_DOMAIN)
                    && !key.equals(STRIMZI_KIND_LABEL)) {
                throw new IllegalArgumentException("User labels includes a Strimzi label that is not "
                        + STRIMZI_KIND_LABEL + ": " + key);
            }
        }
        return new Labels(userLabels);
    }

    /**
     * Returns the labels of the given {@code resource}.
     */
    public static Labels fromResource(HasMetadata resource) {
        return new Labels(resource.getMetadata().getLabels());
    }

    private Labels(Map<String, String> labels) {
        this.labels = unmodifiableMap(new HashMap(labels));
    }

    private Labels with(String label, String value) {
        Map<String, String> newLabels = new HashMap<>(labels.size() + 1);
        newLabels.putAll(labels);
        newLabels.put(label, value);
        return new Labels(newLabels);
    }

    private Labels without(String label) {
        Map<String, String> newLabels = new HashMap<>(labels);
        newLabels.remove(label);
        return new Labels(newLabels);
    }

    /**
     * The same labels as this instance, but with the given {@code type} for the {@code strimzi.io/type} key.
     */
    public Labels withType(String type) {
        return with(STRIMZI_TYPE_LABEL, type);
    }

    /**
     * The same labels as this instance, but without any {@code strimzi.io/type} key.
     */
    public Labels withoutType() {
        return without(STRIMZI_TYPE_LABEL);
    }

    /**
     * The same labels as this instance, but with the given {@code kind} for the {@code strimzi.io/kind} key.
     */
    public Labels withKind(String kind) {
        return with(STRIMZI_KIND_LABEL, kind);
    }

    /**
     * The same labels as this instance, but without any {@code strimzi.io/kind} key.
     */
    public Labels withoutKind() {
        return without(STRIMZI_KIND_LABEL);
    }

    /**
     * The same labels as this instance, but with the given {@code cluster} for the {@code strimzi.io/cluster} key.
     */
    public Labels withCluster(String cluster) {
        return with(STRIMZI_CLUSTER_LABEL, cluster);
    }

    /**
     * The same labels as this instance, but with the given {@code name} for the {@code strimzi.io/name} key.
     */
    public Labels withName(String name) {
        return with(STRIMZI_NAME_LABEL, name);
    }

    /**
     * An unmodifiable map of the labels.
     */
    public Map<String, String> toMap() {
        return labels;
    }

    /**
     * A singleton instance with the given {@code cluster} for the {@code strimzi.io/cluster} key.
     */
    public static Labels forCluster(String cluster) {
        return new Labels(singletonMap(STRIMZI_CLUSTER_LABEL, cluster));
    }

    /**
     * A singleton instance with the given {@code type} for the {@code strimzi.io/type} key.
     */
    public static Labels forType(String type) {
        return new Labels(singletonMap(STRIMZI_TYPE_LABEL, type));
    }

    /**
     * A singleton instance with the given {@code kind} for the {@code strimzi.io/kind} key.
     */
    public static Labels forKind(String kind) {
        return new Labels(singletonMap(STRIMZI_KIND_LABEL, kind));
    }

    /**
     * Return the value of the {@code strimzi.io/type}.
     */
    public String type() {
        return labels.get(STRIMZI_TYPE_LABEL);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Labels labels1 = (Labels) o;
        return Objects.equals(labels, labels1.labels);
    }

    @Override
    public int hashCode() {

        return Objects.hash(labels);
    }

    @Override
    public String toString() {
        return "Labels" + labels;
    }
}
