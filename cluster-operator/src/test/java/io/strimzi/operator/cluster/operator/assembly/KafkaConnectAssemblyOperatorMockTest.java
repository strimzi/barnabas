/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.operator.assembly;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.strimzi.api.kafka.Crds;
import io.strimzi.api.kafka.model.DoneableKafkaConnect;
import io.strimzi.api.kafka.KafkaConnectAssemblyList;
import io.strimzi.api.kafka.model.KafkaConnect;
import io.strimzi.api.kafka.model.KafkaConnectBuilder;
import io.strimzi.operator.cluster.model.KafkaConnectCluster;
import io.strimzi.operator.common.Reconciliation;
import io.strimzi.operator.common.model.Labels;
import io.strimzi.operator.common.model.ResourceType;
import io.strimzi.operator.common.operator.MockCertManager;
import io.strimzi.operator.common.operator.resource.ConfigMapOperator;
import io.strimzi.operator.common.operator.resource.CrdOperator;
import io.strimzi.operator.common.operator.resource.DeploymentOperator;
import io.strimzi.operator.common.operator.resource.SecretOperator;
import io.strimzi.operator.common.operator.resource.ServiceOperator;
import io.strimzi.test.TestUtils;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@RunWith(VertxUnitRunner.class)
public class KafkaConnectAssemblyOperatorMockTest {

    private static final Logger LOGGER = LogManager.getLogger(KafkaConnectAssemblyOperatorMockTest.class);

    private static final String NAMESPACE = "my-namespace";
    private static final String CLUSTER_NAME = "my-connect-cluster";

    private final int replicas = 3;

    private KubernetesClient mockClient;

    private Vertx vertx;
    private KafkaConnect cluster;

    @Before
    public void before() {
        this.vertx = Vertx.vertx();

        this.cluster = new KafkaConnectBuilder()
                .withMetadata(new ObjectMetaBuilder()
                    .withName(CLUSTER_NAME)
                    .withNamespace(NAMESPACE)
                    .withLabels(Labels.userLabels(TestUtils.map("foo", "bar")).toMap())
                    .build())
                .withNewSpec()
                    .withReplicas(replicas)
                .endSpec()
            .build();
        mockClient = new MockKube().withCustomResourceDefinition(Crds.kafkaConnect(), KafkaConnect.class, KafkaConnectAssemblyList.class, DoneableKafkaConnect.class)
                .withInitialInstances(Collections.singleton(cluster)).end().build();
    }

    @After
    public void after() {
        this.vertx.close();
    }

    private KafkaConnectAssemblyOperator createConnectCluster(TestContext context) {
        CrdOperator<KubernetesClient, KafkaConnect, KafkaConnectAssemblyList, DoneableKafkaConnect>
                connectOperator = new CrdOperator<>(vertx, mockClient,
                KafkaConnect.class, KafkaConnectAssemblyList.class, DoneableKafkaConnect.class);
        ConfigMapOperator cmops = new ConfigMapOperator(vertx, mockClient);
        ServiceOperator svcops = new ServiceOperator(vertx, mockClient);
        DeploymentOperator depops = new DeploymentOperator(vertx, mockClient);
        SecretOperator secretops = new SecretOperator(vertx, mockClient);
        KafkaConnectAssemblyOperator kco = new KafkaConnectAssemblyOperator(vertx, true,
                new MockCertManager(),
                connectOperator,
                cmops, depops, svcops, secretops);

        LOGGER.info("Reconciling initially -> create");
        Async createAsync = context.async();
        kco.reconcileAssembly(new Reconciliation("test-trigger", ResourceType.CONNECT, NAMESPACE, CLUSTER_NAME), ar -> {
            if (ar.failed()) ar.cause().printStackTrace();
            context.assertTrue(ar.succeeded());
            context.assertNotNull(mockClient.extensions().deployments().inNamespace(NAMESPACE).withName(KafkaConnectCluster.kafkaConnectClusterName(CLUSTER_NAME)).get());
            context.assertNotNull(mockClient.configMaps().inNamespace(NAMESPACE).withName(KafkaConnectCluster.logAndMetricsConfigName(CLUSTER_NAME)).get());
            context.assertNotNull(mockClient.services().inNamespace(NAMESPACE).withName(KafkaConnectCluster.serviceName(CLUSTER_NAME)).get());
            createAsync.complete();
        });
        createAsync.await();
        return kco;
    }

    /** Create a cluster from a Kafka Cluster CM */
    @Test
    public void testCreateUpdateDelete(TestContext context) {
        KafkaConnectAssemblyOperator kco = createConnectCluster(context);
        LOGGER.info("Reconciling again -> update");
        Async updateAsync = context.async();
        kco.reconcileAssembly(new Reconciliation("test-trigger", ResourceType.CONNECT, NAMESPACE, CLUSTER_NAME), ar -> {
            if (ar.failed()) ar.cause().printStackTrace();
            context.assertTrue(ar.succeeded());
            updateAsync.complete();
        });
        updateAsync.await();
        LOGGER.info("Reconciling again -> delete");
        mockClient.customResources(Crds.kafkaConnect(),
                KafkaConnect.class, KafkaConnectAssemblyList.class, DoneableKafkaConnect.class).
                inNamespace(NAMESPACE).withName(CLUSTER_NAME).delete();
        Async deleteAsync = context.async();
        kco.reconcileAssembly(new Reconciliation("test-trigger", ResourceType.CONNECT, NAMESPACE, CLUSTER_NAME), ar -> {
            if (ar.failed()) ar.cause().printStackTrace();
            context.assertTrue(ar.succeeded());
            // TODO: Should verify that all resources were removed from MockKube
            context.assertNull(mockClient.extensions().deployments().inNamespace(NAMESPACE).withName(KafkaConnectCluster.kafkaConnectClusterName(CLUSTER_NAME)).get());
            context.assertNull(mockClient.configMaps().inNamespace(NAMESPACE).withName(KafkaConnectCluster.logAndMetricsConfigName(CLUSTER_NAME)).get());
            context.assertNull(mockClient.services().inNamespace(NAMESPACE).withName(KafkaConnectCluster.serviceName(CLUSTER_NAME)).get());
            deleteAsync.complete();
        });
    }

    /** Test that the cluster resources will be discovered even when the DELETE event is missed */
    @Test
    public void testReconcileAllDeleteCase(TestContext context) throws InterruptedException {
        KafkaConnectAssemblyOperator kco = createConnectCluster(context);
        LOGGER.info("Reconciling again -> delete");
        mockClient.customResources(Crds.kafkaConnect(),
                KafkaConnect.class, KafkaConnectAssemblyList.class, DoneableKafkaConnect.class).
                inNamespace(NAMESPACE).withName(CLUSTER_NAME).delete();
        kco.reconcileAll("test-trigger", NAMESPACE).await(60, TimeUnit.SECONDS);

        // TODO: Should verify that all resources were removed from MockKube
        context.assertNull(mockClient.extensions().deployments().inNamespace(NAMESPACE).withName(KafkaConnectCluster.kafkaConnectClusterName(CLUSTER_NAME)).get());
        context.assertNull(mockClient.configMaps().inNamespace(NAMESPACE).withName(KafkaConnectCluster.logAndMetricsConfigName(CLUSTER_NAME)).get());
        context.assertNull(mockClient.services().inNamespace(NAMESPACE).withName(KafkaConnectCluster.serviceName(CLUSTER_NAME)).get());
    }
}
