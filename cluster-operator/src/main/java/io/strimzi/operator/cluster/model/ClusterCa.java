/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.model;

import io.fabric8.kubernetes.api.model.Secret;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.certs.CertAndKey;
import io.strimzi.certs.CertManager;
import io.strimzi.certs.Subject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ClusterCa extends Ca {

    // the Kubernetes service DNS domain is customizable on cluster creation but it's "cluster.local" by default
    // there is no clean way to get it from a running application so we are passing it through an env var
    public static final String KUBERNETES_SERVICE_DNS_DOMAIN =
            System.getenv().getOrDefault("KUBERNETES_SERVICE_DNS_DOMAIN", "cluster.local");
    private final String clusterName;
    private Secret entityOperatorSecret;
    private Secret topicOperatorSecret;

    private Secret brokersSecret;
    private Secret zkNodesSecret;

    public ClusterCa(CertManager certManager, String clusterName, Secret caCertSecret, Secret caKeySecret) {
        this(certManager, clusterName, caCertSecret, caKeySecret, 365, 30, true);
    }

    public ClusterCa(CertManager certManager,
                     String clusterName,
                     Secret clusterCaCert,
                     Secret clusterCaKey,
                     int validityDays,
                     int renewalDays,
                     boolean generateCa) {
        super(certManager, AbstractModel.getClusterCaName(clusterName), clusterCaCert,
                AbstractModel.getClusterCaKeyName(clusterName), clusterCaKey,
                validityDays, renewalDays, generateCa);
        this.clusterName = clusterName;
    }

    public void sort(List<Secret> secrets) {
        Secret brokersSecret = null;
        Secret eoSecrets = null;
        Secret toSecret = null;
        Secret zkNodesSecret = null;
        for (Secret secret: secrets) {
            String name = secret.getMetadata().getName();
            if (KafkaCluster.brokersSecretName(clusterName).equals(name)) {
                brokersSecret = secret;
            } else if (EntityOperator.secretName(clusterName).equals(name)) {
                eoSecrets = secret;
            } else if (TopicOperator.secretName(clusterName).equals(name)) {
                toSecret = secret;
            } else if (ZookeeperCluster.nodesSecretName(clusterName).equals(name)) {
                zkNodesSecret = secret;
            }
        }
        this.brokersSecret = brokersSecret;
        this.entityOperatorSecret = eoSecrets;
        this.topicOperatorSecret = toSecret;
        this.zkNodesSecret = zkNodesSecret;
    }

    public Secret topicOperatorSecret() {
        return topicOperatorSecret;
    }

    public Secret entityOperatorSecret() {
        return entityOperatorSecret;
    }

    public Map<String, CertAndKey> generateZkCerts(Kafka kafka) throws IOException {
        String cluster = kafka.getMetadata().getName();
        String namespace = kafka.getMetadata().getNamespace();
        Function<Integer, Subject> subjectFn = i -> {
            Map<String, String> sbjAltNames = new HashMap<>();
            sbjAltNames.put("DNS.1", ZookeeperCluster.serviceName(cluster));
            sbjAltNames.put("DNS.2", String.format("%s.%s", ZookeeperCluster.serviceName(cluster), namespace));
            sbjAltNames.put("DNS.3", String.format("%s.%s.svc.%s", ZookeeperCluster.serviceName(cluster), namespace, KUBERNETES_SERVICE_DNS_DOMAIN));
            sbjAltNames.put("DNS.4", String.format("%s.%s.%s.svc.%s", ZookeeperCluster.zookeeperPodName(cluster, i), ZookeeperCluster.headlessServiceName(cluster), namespace, KUBERNETES_SERVICE_DNS_DOMAIN));

            Subject subject = new Subject();
            subject.setOrganizationName("io.strimzi");
            subject.setCommonName(ZookeeperCluster.zookeeperClusterName(cluster));
            subject.setSubjectAltNames(sbjAltNames);

            return subject;
        };

        log.debug("Cluster communication certificates");
        return maybeCopyOrGenerateCerts(
            kafka.getSpec().getZookeeper().getReplicas(),
            subjectFn,
            zkNodesSecret,
            podNum -> ZookeeperCluster.zookeeperPodName(cluster, podNum));
    }

    public Map<String, CertAndKey> generateBrokerCerts(Kafka kafka, String externalBootstrapAddress, Map<Integer, String> externalAddresses) throws IOException {
        String cluster = kafka.getMetadata().getName();
        String namespace = kafka.getMetadata().getNamespace();
        log.debug("Internal communication certificates");
        Function<Integer, Subject> subjectFn = i -> {
            Map<String, String> sbjAltNames = new HashMap<>();
            sbjAltNames.put("DNS.1", KafkaCluster.serviceName(cluster));
            sbjAltNames.put("DNS.2", String.format("%s.%s", KafkaCluster.serviceName(cluster), namespace));
            sbjAltNames.put("DNS.3", String.format("%s.%s.svc.%s", KafkaCluster.serviceName(cluster), namespace, KUBERNETES_SERVICE_DNS_DOMAIN));
            sbjAltNames.put("DNS.4", String.format("%s.%s.%s.svc.%s", KafkaCluster.kafkaPodName(cluster, i), KafkaCluster.headlessServiceName(cluster), namespace, KUBERNETES_SERVICE_DNS_DOMAIN));
            int nextDnsId = 5;
            if (externalBootstrapAddress != null)   {
                sbjAltNames.put("DNS." + nextDnsId, externalBootstrapAddress);
                nextDnsId++;
            }

            if (externalAddresses.get(i) != null)   {
                sbjAltNames.put("DNS." + nextDnsId, externalAddresses.get(i));
                nextDnsId++;
            }

            Subject subject = new Subject();
            subject.setOrganizationName("io.strimzi");
            subject.setCommonName(KafkaCluster.kafkaClusterName(cluster));
            subject.setSubjectAltNames(sbjAltNames);

            return subject;
        };

        return maybeCopyOrGenerateCerts(
            kafka.getSpec().getKafka().getReplicas(),
            subjectFn,
            brokersSecret,
            podNum -> KafkaCluster.kafkaPodName(cluster, podNum));
    }

}
