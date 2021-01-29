/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.systemtest.resources.crd;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.strimzi.api.kafka.Crds;
import io.strimzi.api.kafka.KafkaMirrorMaker2List;
import io.strimzi.api.kafka.model.CertSecretSourceBuilder;
import io.strimzi.api.kafka.model.KafkaMirrorMaker2;
import io.strimzi.api.kafka.model.KafkaMirrorMaker2Builder;
import io.strimzi.api.kafka.model.KafkaMirrorMaker2ClusterSpec;
import io.strimzi.api.kafka.model.KafkaMirrorMaker2ClusterSpecBuilder;
import io.strimzi.api.kafka.model.KafkaResources;
import io.strimzi.systemtest.Constants;
import io.strimzi.systemtest.Environment;
import io.strimzi.systemtest.resources.ResourceType;
import io.strimzi.systemtest.utils.kafkaUtils.KafkaMirrorMaker2Utils;
import io.strimzi.test.TestUtils;
import io.strimzi.systemtest.resources.ResourceManager;
import io.strimzi.test.k8s.KubeClusterResource;

import java.util.function.Consumer;

import static io.strimzi.systemtest.enums.CustomResourceStatus.Ready;
import static io.strimzi.systemtest.resources.ResourceManager.CR_CREATION_TIMEOUT;
import static io.strimzi.systemtest.resources.ResourceManager.kubeClient;

public class KafkaMirrorMaker2Resource implements ResourceType<KafkaMirrorMaker2> {

    @Override
    public String getKind() {
        return KafkaMirrorMaker2.RESOURCE_KIND;
    }
    @Override
    public KafkaMirrorMaker2 get(String namespace, String name) {
        return kafkaMirrorMaker2Client().inNamespace(namespace).withName(name).get();
    }
    @Override
    public void create(KafkaMirrorMaker2 resource) {
        kafkaMirrorMaker2Client().inNamespace(resource.getMetadata().getNamespace()).createOrReplace(resource);
    }
    @Override
    public void delete(KafkaMirrorMaker2 resource) throws Exception {
        kafkaMirrorMaker2Client().inNamespace(resource.getMetadata().getNamespace()).withName(
            resource.getMetadata().getName()).withPropagationPolicy(DeletionPropagation.FOREGROUND).delete();
    }
    @Override
    public boolean isReady(KafkaMirrorMaker2 resource) {
        return KafkaMirrorMaker2Utils.waitForKafkaMirrorMaker2Ready(resource.getMetadata().getName());
    }
    @Override
    public void refreshResource(KafkaMirrorMaker2 existing, KafkaMirrorMaker2 newResource) {
        existing.setMetadata(newResource.getMetadata());
        existing.setSpec(newResource.getSpec());
        existing.setStatus(newResource.getStatus());
    }
    public static MixedOperation<KafkaMirrorMaker2, KafkaMirrorMaker2List, Resource<KafkaMirrorMaker2>> kafkaMirrorMaker2Client() {
        return Crds.kafkaMirrorMaker2V1Beta2Operation(ResourceManager.kubeClient().getClient());
    }

    public static void replaceKafkaMirrorMaker2Resource(String resourceName, Consumer<KafkaMirrorMaker2> editor) {
        ResourceManager.replaceCrdResource(KafkaMirrorMaker2.class, KafkaMirrorMaker2List.class, resourceName, editor);
    }

}
