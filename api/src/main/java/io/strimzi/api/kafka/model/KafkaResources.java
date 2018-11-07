/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.api.kafka.model;

/**
 * Encapsulates the naming scheme used for the resources which the Cluster Operator manages for a
 * {@code Kafka} cluster.
 */
public class KafkaResources {
    private KafkaResources() { }

    /**
     * Returns the name of the Zookeeper {@code StatefulSet} for a {@code Kafka} cluster of the given name.
     * @param clusterName  The {@code metadata.name} of the {@code Kafka} resource.
     * @return The name of the corresponding Zookeeper {@code StatefulSet}.
     */
    public static String zookeeperStatefulSetName(String clusterName) {
        return clusterName + "-zookeeper";
    }

    /**
     * Returns the name of the Zookeeper {@code Pod} for a {@code Kafka} cluster of the given name.
     * @param clusterName  The {@code metadata.name} of the {@code Kafka} resource.
     * @param podNum The number of the Zookeeper pod
     * @return The name of the corresponding Zookeeper {@code Pod}.
     */
    public static String zookeeperPodName(String clusterName, int podNum) {
        return zookeeperStatefulSetName(clusterName) + "-" + podNum;
    }

    /**
     * Returns the name of the Kafka {@code StatefulSet} for a {@code Kafka} cluster of the given name.
     * @param clusterName  The {@code metadata.name} of the {@code Kafka} resource.
     * @return The name of the corresponding Kafka {@code StatefulSet}.
     */
    public static String kafkaStatefulSetName(String clusterName) {
        return clusterName + "-kafka";
    }

    /**
     * Returns the name of the Kafka {@code Pod} for a {@code Kafka} cluster of the given name.
     * @param clusterName  The {@code metadata.name} of the {@code Kafka} resource.
     * @param podNum The number of the Kafka pod
     * @return The name of the corresponding Kafka {@code Pod}.
     */
    public static String kafkaPodName(String clusterName, int podNum) {
        return kafkaStatefulSetName(clusterName) + "-" + podNum;
    }

    /**
     * Returns the name of the Entity Operator {@code Deployment} for a {@code Kafka} cluster of the given name.
     * This {@code Deployment} will only exist if {@code Kafka.spec.entityOperator} is configured in the
     * {@code Kafka} resource with the given name.
     * @param clusterName  The {@code metadata.name} of the {@code Kafka} resource.
     * @return The name of the corresponding Entity Operator {@code Deployment}.
     */
    public static String entityOperatorDeploymentName(String clusterName) {
        return clusterName + "-entity-operator";
    }

    /**
     * Returns the name of the Cluster CA certificate {@code Secret} for a {@code Kafka} cluster of the given name.
     * @param clusterName  The {@code metadata.name} of the {@code Kafka} resource.
     * @return The name of the corresponding Cluster CA certificate {@code Secret}.
     */
    public static String clusterCaCertificateSecretName(String clusterName) {
        return clusterName + "-cluster-ca-cert";
    }

    /**
     * Returns the name of the Cluster CA key {@code Secret} for a {@code Kafka} cluster of the given name.
     * @param clusterName  The {@code metadata.name} of the {@code Kafka} resource.
     * @return The name of the corresponding Cluster CA key {@code Secret}.
     */
    public static String clusterCaKeySecretName(String clusterName) {
        return clusterName + "-cluster-ca";
    }

    /**
     * Returns the name of the Clients CA certificate {@code Secret} for a {@code Kafka} cluster of the given name.
     * @param clusterName  The {@code metadata.name} of the {@code Kafka} resource.
     * @return The name of the corresponding Clients CA certificate {@code Secret}.
     */
    public static String clientsCaCertificateSecretName(String clusterName) {
        return clusterName + "-clients-ca-cert";
    }

    /**
     * Returns the name of the Clients CA key {@code Secret} for a {@code Kafka} cluster of the given name.
     * @param clusterName  The {@code metadata.name} of the {@code Kafka} resource.
     * @return The name of the corresponding Clients CA key {@code Secret}.
     */
    public static String clientsCaKeySecretName(String clusterName) {
        return clusterName + "-clients-ca";
    }

    /**
     * Returns the name of the internal bootstrap {@code Service} for a {@code Kafka} cluster of the given name.
     * @param clusterName  The {@code metadata.name} of the {@code Kafka} resource.
     * @return The name of the corresponding bootstrap {@code Service}.
     */
    public static String bootstrapServiceName(String clusterName) {
        return clusterName + "-kafka-bootstrap";
    }

    /**
     * Returns the address (<em>&lt;host&gt;</em>:<em>&lt;port&gt;</em>)
     * of the internal plain bootstrap {@code Service} for a {@code Kafka} cluster of the given name.
     * @param clusterName  The {@code metadata.name} of the {@code Kafka} resource.
     * @return The address of the corresponding bootstrap {@code Service}.
     * @see #tlsBootstrapConnection(String)
     */
    public static String plainBootstrapConnection(String clusterName) {
        return bootstrapServiceName(clusterName) + ":9092";
    }

    /**
     * Returns the address (<em>&lt;host&gt;</em>:<em>&lt;port&gt;</em>)
     * of the internal TLS bootstrap {@code Service} for a {@code Kafka} cluster of the given name.
     * @param clusterName  The {@code metadata.name} of the {@code Kafka} resource.
     * @return The address of the corresponding bootstrap {@code Service}.
     * @see #plainBootstrapConnection(String)
     */
    public static String tlsBootstrapConnection(String clusterName) {
        return bootstrapServiceName(clusterName) + ":9093";
    }

    /**
     * Returns the name of the (headless) brokers {@code Service} for a {@code Kafka} cluster of the given name.
     * @param clusterName  The {@code metadata.name} of the {@code Kafka} resource.
     * @return The name of the corresponding brokers {@code Service}.
     */
    public static String brokersServiceName(String clusterName) {
        return clusterName + "-kafka-brokers";
    }

    /**
     * Returns the name of the external bootstrap {@code Service} for a {@code Kafka} cluster of the given name.
     * This {@code Service} will only exist if {@code Kafka.spec.kafka.listeners.external} is configured for a
     * loadbalancer or NodePort in the {@code Kafka} resource with the given name.
     * @param clusterName  The {@code metadata.name} of the {@code Kafka} resource.
     * @return The name of the corresponding bootstrap {@code Service}.
     */
    public static String externalBootstrapServiceName(String clusterName) {
        return clusterName + "-kafka-external-bootstrap";
    }

    /**
     * Returns the name of the Kafka metrics and log {@code ConfigMap} for a {@code Kafka} cluster of the given name.
     * @param clusterName  The {@code metadata.name} of the {@code Kafka} resource.
     * @return The name of the corresponding Kafka metrics and log {@code ConfigMap}.
     */
    public static String kafkaMetricsAndLogConfigMapName(String clusterName) {
        return clusterName + "-kafka-config";
    }

    /**
     * Returns the name of the Zookeeper metrics and log {@code ConfigMap} for a {@code Kafka} cluster of the given name.
     * @param clusterName  The {@code metadata.name} of the {@code Kafka} resource.
     * @return The name of the corresponding Zookeeper metrics and log {@code ConfigMap}.
     */
    public static String zookeeperMetricsAndLogConfigMapName(String clusterName) {
        return clusterName + "-zookeeper-config";
    }
}
