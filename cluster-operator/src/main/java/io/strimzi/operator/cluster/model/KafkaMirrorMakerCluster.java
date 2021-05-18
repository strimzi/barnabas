/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.model;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.SecurityContext;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.policy.PodDisruptionBudget;
import io.strimzi.api.kafka.model.CertSecretSource;
import io.strimzi.api.kafka.model.ContainerEnvVar;
import io.strimzi.api.kafka.model.KafkaMirrorMaker;
import io.strimzi.api.kafka.model.KafkaMirrorMakerClientSpec;
import io.strimzi.api.kafka.model.KafkaMirrorMakerConsumerSpec;
import io.strimzi.api.kafka.model.KafkaMirrorMakerProducerSpec;
import io.strimzi.api.kafka.model.KafkaMirrorMakerResources;
import io.strimzi.api.kafka.model.KafkaMirrorMakerSpec;
import io.strimzi.api.kafka.model.Probe;
import io.strimzi.api.kafka.model.ProbeBuilder;
import io.strimzi.api.kafka.model.template.KafkaMirrorMakerTemplate;
import io.strimzi.api.kafka.model.tracing.Tracing;
import io.strimzi.operator.common.Reconciliation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class KafkaMirrorMakerCluster extends AbstractModel {
    protected static final String APPLICATION_NAME = "kafka-mirror-maker";

    protected static final String TLS_CERTS_VOLUME_MOUNT_CONSUMER = "/opt/kafka/consumer-certs/";
    protected static final String PASSWORD_VOLUME_MOUNT_CONSUMER = "/opt/kafka/consumer-password/";
    protected static final String TLS_CERTS_VOLUME_MOUNT_PRODUCER = "/opt/kafka/producer-certs/";
    protected static final String PASSWORD_VOLUME_MOUNT_PRODUCER = "/opt/kafka/producer-password/";
    protected static final String OAUTH_TLS_CERTS_BASE_VOLUME_MOUNT_CONSUMER = "/opt/kafka/consumer-oauth-certs/";
    protected static final String OAUTH_TLS_CERTS_BASE_VOLUME_MOUNT_PRODUCER = "/opt/kafka/producer-oauth-certs/";

    // Configuration defaults
    private static final int DEFAULT_HEALTHCHECK_DELAY = 60;
    private static final int DEFAULT_HEALTHCHECK_TIMEOUT = 5;
    private static final int DEFAULT_HEALTHCHECK_PERIOD = 10;
    public static final Probe READINESS_PROBE_OPTIONS = new ProbeBuilder().withTimeoutSeconds(DEFAULT_HEALTHCHECK_TIMEOUT).withInitialDelaySeconds(DEFAULT_HEALTHCHECK_DELAY).build();
    protected static final boolean DEFAULT_KAFKA_MIRRORMAKER_METRICS_ENABLED = false;

    // Kafka Mirror Maker configuration keys (EnvVariables)
    protected static final String ENV_VAR_PREFIX = "KAFKA_MIRRORMAKER_";

    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_METRICS_ENABLED = "KAFKA_MIRRORMAKER_METRICS_ENABLED";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_BOOTSTRAP_SERVERS_CONSUMER = "KAFKA_MIRRORMAKER_BOOTSTRAP_SERVERS_CONSUMER";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_TLS_CONSUMER = "KAFKA_MIRRORMAKER_TLS_CONSUMER";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_TRUSTED_CERTS_CONSUMER = "KAFKA_MIRRORMAKER_TRUSTED_CERTS_CONSUMER";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_TLS_AUTH_CERT_CONSUMER = "KAFKA_MIRRORMAKER_TLS_AUTH_CERT_CONSUMER";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_TLS_AUTH_KEY_CONSUMER = "KAFKA_MIRRORMAKER_TLS_AUTH_KEY_CONSUMER";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_SASL_MECHANISM_CONSUMER = "KAFKA_MIRRORMAKER_SASL_MECHANISM_CONSUMER";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_SASL_PASSWORD_FILE_CONSUMER = "KAFKA_MIRRORMAKER_SASL_PASSWORD_FILE_CONSUMER";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_SASL_USERNAME_CONSUMER = "KAFKA_MIRRORMAKER_SASL_USERNAME_CONSUMER";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_GROUPID_CONSUMER = "KAFKA_MIRRORMAKER_GROUPID_CONSUMER";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_CONFIGURATION_CONSUMER = "KAFKA_MIRRORMAKER_CONFIGURATION_CONSUMER";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_OAUTH_CONFIG_CONSUMER = "KAFKA_MIRRORMAKER_OAUTH_CONFIG_CONSUMER";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_OAUTH_CLIENT_SECRET_CONSUMER = "KAFKA_MIRRORMAKER_OAUTH_CLIENT_SECRET_CONSUMER";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_OAUTH_ACCESS_TOKEN_CONSUMER = "KAFKA_MIRRORMAKER_OAUTH_ACCESS_TOKEN_CONSUMER";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_OAUTH_REFRESH_TOKEN_CONSUMER = "KAFKA_MIRRORMAKER_OAUTH_REFRESH_TOKEN_CONSUMER";

    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_BOOTSTRAP_SERVERS_PRODUCER = "KAFKA_MIRRORMAKER_BOOTSTRAP_SERVERS_PRODUCER";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_TLS_PRODUCER = "KAFKA_MIRRORMAKER_TLS_PRODUCER";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_TRUSTED_CERTS_PRODUCER = "KAFKA_MIRRORMAKER_TRUSTED_CERTS_PRODUCER";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_TLS_AUTH_CERT_PRODUCER = "KAFKA_MIRRORMAKER_TLS_AUTH_CERT_PRODUCER";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_TLS_AUTH_KEY_PRODUCER = "KAFKA_MIRRORMAKER_TLS_AUTH_KEY_PRODUCER";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_SASL_MECHANISM_PRODUCER = "KAFKA_MIRRORMAKER_SASL_MECHANISM_PRODUCER";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_SASL_PASSWORD_FILE_PRODUCER = "KAFKA_MIRRORMAKER_SASL_PASSWORD_FILE_PRODUCER";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_SASL_USERNAME_PRODUCER = "KAFKA_MIRRORMAKER_SASL_USERNAME_PRODUCER";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_CONFIGURATION_PRODUCER = "KAFKA_MIRRORMAKER_CONFIGURATION_PRODUCER";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_OAUTH_CONFIG_PRODUCER = "KAFKA_MIRRORMAKER_OAUTH_CONFIG_PRODUCER";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_OAUTH_CLIENT_SECRET_PRODUCER = "KAFKA_MIRRORMAKER_OAUTH_CLIENT_SECRET_PRODUCER";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_OAUTH_ACCESS_TOKEN_PRODUCER = "KAFKA_MIRRORMAKER_OAUTH_ACCESS_TOKEN_PRODUCER";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_OAUTH_REFRESH_TOKEN_PRODUCER = "KAFKA_MIRRORMAKER_OAUTH_REFRESH_TOKEN_PRODUCER";

    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_INCLUDE = "KAFKA_MIRRORMAKER_INCLUDE";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_NUMSTREAMS = "KAFKA_MIRRORMAKER_NUMSTREAMS";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_OFFSET_COMMIT_INTERVAL = "KAFKA_MIRRORMAKER_OFFSET_COMMIT_INTERVAL";
    protected static final String ENV_VAR_KAFKA_MIRRORMAKER_ABORT_ON_SEND_FAILURE = "KAFKA_MIRRORMAKER_ABORT_ON_SEND_FAILURE";

    protected static final String ENV_VAR_STRIMZI_READINESS_PERIOD = "STRIMZI_READINESS_PERIOD";
    protected static final String ENV_VAR_STRIMZI_LIVENESS_PERIOD = "STRIMZI_LIVENESS_PERIOD";
    protected static final String ENV_VAR_STRIMZI_TRACING = "STRIMZI_TRACING";

    protected String include;
    protected Tracing tracing;

    protected KafkaMirrorMakerProducerSpec producer;
    protected KafkaMirrorMakerConsumerSpec consumer;
    protected List<ContainerEnvVar> templateContainerEnvVars;
    protected SecurityContext templateContainerSecurityContext;

    /**
     * Constructor
     *
     * @param resource Kubernetes resource with metadata containing the namespace and cluster name
     */
    protected KafkaMirrorMakerCluster(HasMetadata resource) {
        super(resource, APPLICATION_NAME);
        this.name = KafkaMirrorMakerResources.deploymentName(cluster);
        this.serviceName = KafkaMirrorMakerResources.serviceName(cluster);
        this.ancillaryConfigMapName = KafkaMirrorMakerResources.metricsAndLogConfigMapName(cluster);
        this.readinessPath = "/";
        this.readinessProbeOptions = READINESS_PROBE_OPTIONS;
        this.livenessPath = "/";
        this.livenessProbeOptions = READINESS_PROBE_OPTIONS;
        this.isMetricsEnabled = DEFAULT_KAFKA_MIRRORMAKER_METRICS_ENABLED;

        this.mountPath = "/var/lib/kafka";
        this.logAndMetricsConfigVolumeName = "kafka-metrics-and-logging";
        this.logAndMetricsConfigMountPath = "/opt/kafka/custom-config/";
    }

    @SuppressWarnings("deprecation")
    public static KafkaMirrorMakerCluster fromCrd(Reconciliation reconciliation, KafkaMirrorMaker kafkaMirrorMaker, KafkaVersion.Lookup versions) {
        KafkaMirrorMakerCluster kafkaMirrorMakerCluster = new KafkaMirrorMakerCluster(kafkaMirrorMaker);

        KafkaMirrorMakerSpec spec = kafkaMirrorMaker.getSpec();
        if (spec != null) {
            kafkaMirrorMakerCluster.setReplicas(spec.getReplicas());
            kafkaMirrorMakerCluster.setResources(spec.getResources());

            if (spec.getReadinessProbe() != null) {
                kafkaMirrorMakerCluster.setReadinessProbe(spec.getReadinessProbe());
            }

            if (spec.getLivenessProbe() != null) {
                kafkaMirrorMakerCluster.setLivenessProbe(spec.getLivenessProbe());
            }

            String whitelist = spec.getWhitelist();
            String include = spec.getInclude();

            if (include == null && whitelist == null)   {
                throw new InvalidResourceException("One of the fields include or whitelist needs to be specified.");
            } else if (whitelist != null && include != null) {
                log.warn("Both include and whitelist fields are present. Whitelist is deprecated and will be ignored.");
            }

            kafkaMirrorMakerCluster.setInclude(include != null ? include : whitelist);

            AuthenticationUtils.validateClientAuthentication(reconciliation, spec.getProducer().getAuthentication(), spec.getProducer().getTls() != null);
            kafkaMirrorMakerCluster.setProducer(spec.getProducer());
            AuthenticationUtils.validateClientAuthentication(reconciliation, spec.getConsumer().getAuthentication(), spec.getConsumer().getTls() != null);
            kafkaMirrorMakerCluster.setConsumer(spec.getConsumer());

            kafkaMirrorMakerCluster.setImage(versions.kafkaMirrorMakerImage(spec.getImage(), spec.getVersion()));

            kafkaMirrorMakerCluster.setLogging(spec.getLogging());
            kafkaMirrorMakerCluster.setGcLoggingEnabled(spec.getJvmOptions() == null ? DEFAULT_JVM_GC_LOGGING_ENABLED : spec.getJvmOptions().isGcLoggingEnabled());
            if (spec.getJvmOptions() != null) {
                kafkaMirrorMakerCluster.setJavaSystemProperties(spec.getJvmOptions().getJavaSystemProperties());
            }
            kafkaMirrorMakerCluster.setJvmOptions(spec.getJvmOptions());

            // Parse different types of metrics configurations
            ModelUtils.parseMetrics(kafkaMirrorMakerCluster, spec);

            if (spec.getTemplate() != null) {
                KafkaMirrorMakerTemplate template = spec.getTemplate();

                if (template.getMirrorMakerContainer() != null && template.getMirrorMakerContainer().getEnv() != null) {
                    kafkaMirrorMakerCluster.templateContainerEnvVars = template.getMirrorMakerContainer().getEnv();
                }

                if (template.getMirrorMakerContainer() != null && template.getMirrorMakerContainer().getSecurityContext() != null) {
                    kafkaMirrorMakerCluster.templateContainerSecurityContext = template.getMirrorMakerContainer().getSecurityContext();
                }

                if (template.getServiceAccount() != null && template.getServiceAccount().getMetadata() != null) {
                    kafkaMirrorMakerCluster.templateServiceAccountLabels = template.getServiceAccount().getMetadata().getLabels();
                    kafkaMirrorMakerCluster.templateServiceAccountAnnotations = template.getServiceAccount().getMetadata().getAnnotations();
                }

                ModelUtils.parseDeploymentTemplate(kafkaMirrorMakerCluster, template.getDeployment());
                ModelUtils.parsePodTemplate(kafkaMirrorMakerCluster, template.getPod());
                ModelUtils.parsePodDisruptionBudgetTemplate(kafkaMirrorMakerCluster, template.getPodDisruptionBudget());
            }

            kafkaMirrorMakerCluster.tracing = spec.getTracing();
        }

        kafkaMirrorMakerCluster.setOwnerReference(kafkaMirrorMaker);

        return kafkaMirrorMakerCluster;
    }

    protected List<ContainerPort> getContainerPortList(Reconciliation reconciliation) {
        List<ContainerPort> portList = new ArrayList<>(1);
        if (isMetricsEnabled) {
            portList.add(createContainerPort(reconciliation, METRICS_PORT_NAME, METRICS_PORT, "TCP"));
        }

        return portList;
    }

    protected List<Volume> getVolumes(Reconciliation reconciliation, boolean isOpenShift) {
        List<Volume> volumeList = new ArrayList<>(2);

        volumeList.add(createTempDirVolume());
        volumeList.add(VolumeUtils.createConfigMapVolume(reconciliation, logAndMetricsConfigVolumeName, ancillaryConfigMapName));

        createClientSecretVolume(reconciliation, producer, volumeList, "producer-oauth-certs", isOpenShift);
        createClientSecretVolume(reconciliation, consumer, volumeList, "consumer-oauth-certs", isOpenShift);

        return volumeList;
    }

    protected void createClientSecretVolume(Reconciliation reconciliation, KafkaMirrorMakerClientSpec client, List<Volume> volumeList, String oauthVolumeNamePrefix, boolean isOpenShift) {
        if (client.getTls() != null && client.getTls().getTrustedCertificates() != null && client.getTls().getTrustedCertificates().size() > 0) {
            for (CertSecretSource certSecretSource: client.getTls().getTrustedCertificates()) {
                // skipping if a volume with same Secret name was already added
                if (!volumeList.stream().anyMatch(v -> v.getName().equals(certSecretSource.getSecretName()))) {
                    volumeList.add(VolumeUtils.createSecretVolume(reconciliation, certSecretSource.getSecretName(), certSecretSource.getSecretName(), isOpenShift));
                }
            }
        }

        AuthenticationUtils.configureClientAuthenticationVolumes(reconciliation, client.getAuthentication(), volumeList, oauthVolumeNamePrefix, isOpenShift);
    }

    protected List<VolumeMount> getVolumeMounts(Reconciliation reconciliation) {
        List<VolumeMount> volumeMountList = new ArrayList<>(2);

        volumeMountList.add(createTempDirVolumeMount(reconciliation));
        volumeMountList.add(VolumeUtils.createVolumeMount(reconciliation, logAndMetricsConfigVolumeName, logAndMetricsConfigMountPath));

        /** producer auth*/
        if (producer.getTls() != null && producer.getTls().getTrustedCertificates() != null && producer.getTls().getTrustedCertificates().size() > 0) {
            for (CertSecretSource certSecretSource: producer.getTls().getTrustedCertificates()) {
                // skipping if a volume mount with same Secret name was already added
                if (!volumeMountList.stream().anyMatch(vm -> vm.getName().equals(certSecretSource.getSecretName()))) {
                    volumeMountList.add(VolumeUtils.createVolumeMount(reconciliation, certSecretSource.getSecretName(),
                            TLS_CERTS_VOLUME_MOUNT_PRODUCER + certSecretSource.getSecretName()));
                }
            }
        }

        AuthenticationUtils.configureClientAuthenticationVolumeMounts(reconciliation, producer.getAuthentication(), volumeMountList, TLS_CERTS_VOLUME_MOUNT_PRODUCER, PASSWORD_VOLUME_MOUNT_PRODUCER, OAUTH_TLS_CERTS_BASE_VOLUME_MOUNT_PRODUCER, "producer-oauth-certs");

        /** consumer auth*/
        if (consumer.getTls() != null && consumer.getTls().getTrustedCertificates() != null && consumer.getTls().getTrustedCertificates().size() > 0) {
            for (CertSecretSource certSecretSource: consumer.getTls().getTrustedCertificates()) {
                // skipping if a volume mount with same Secret name was already added
                if (!volumeMountList.stream().anyMatch(vm -> vm.getName().equals(certSecretSource.getSecretName()))) {
                    volumeMountList.add(VolumeUtils.createVolumeMount(reconciliation, certSecretSource.getSecretName(),
                            TLS_CERTS_VOLUME_MOUNT_CONSUMER + certSecretSource.getSecretName()));
                }
            }
        }

        AuthenticationUtils.configureClientAuthenticationVolumeMounts(reconciliation, consumer.getAuthentication(), volumeMountList, TLS_CERTS_VOLUME_MOUNT_CONSUMER, PASSWORD_VOLUME_MOUNT_CONSUMER, OAUTH_TLS_CERTS_BASE_VOLUME_MOUNT_CONSUMER, "consumer-oauth-certs");

        return volumeMountList;
    }

    public Deployment generateDeployment(Reconciliation reconciliation, Map<String, String> annotations, boolean isOpenShift, ImagePullPolicy imagePullPolicy, List<LocalObjectReference> imagePullSecrets) {
        return createDeployment(
                getDeploymentStrategy(),
                Collections.emptyMap(),
                annotations,
                getMergedAffinity(),
                getInitContainers(reconciliation, imagePullPolicy),
                getContainers(reconciliation, imagePullPolicy),
                getVolumes(reconciliation, isOpenShift),
                imagePullSecrets);
    }

    @Override
    protected List<Container> getContainers(Reconciliation reconciliation, ImagePullPolicy imagePullPolicy) {

        List<Container> containers = new ArrayList<>(1);

        Container container = new ContainerBuilder()
                .withName(name)
                .withImage(getImage())
                .withCommand("/opt/kafka/kafka_mirror_maker_run.sh")
                .withEnv(getEnvVars(reconciliation))
                .withPorts(getContainerPortList(reconciliation))
                .withLivenessProbe(ProbeGenerator.defaultBuilder(livenessProbeOptions)
                        .withNewExec()
                            .withCommand("/opt/kafka/kafka_mirror_maker_liveness.sh")
                        .endExec().build())
                .withReadinessProbe(ProbeGenerator.defaultBuilder(readinessProbeOptions)
                        .withNewExec()
                            // The mirror-maker-agent will create /tmp/mirror-maker-ready in the container
                            .withCommand("test", "-f", "/tmp/mirror-maker-ready")
                        .endExec().build())
                .withVolumeMounts(getVolumeMounts(reconciliation))
                .withResources(getResources())
                .withImagePullPolicy(determineImagePullPolicy(imagePullPolicy, getImage()))
                .withSecurityContext(templateContainerSecurityContext)
                .build();

        containers.add(container);

        return containers;
    }

    private KafkaMirrorMakerConsumerConfiguration getConsumerConfiguration(Reconciliation reconciliation) {
        KafkaMirrorMakerConsumerConfiguration config = new KafkaMirrorMakerConsumerConfiguration(reconciliation, consumer.getConfig().entrySet());

        if (tracing != null) {
            config.setConfigOption("interceptor.classes", "io.opentracing.contrib.kafka.TracingConsumerInterceptor");
        }

        return config;
    }

    private KafkaMirrorMakerProducerConfiguration getProducerConfiguration(Reconciliation reconciliation)    {
        KafkaMirrorMakerProducerConfiguration config = new KafkaMirrorMakerProducerConfiguration(reconciliation, producer.getConfig().entrySet());

        if (tracing != null) {
            config.setConfigOption("interceptor.classes", "io.opentracing.contrib.kafka.TracingProducerInterceptor");
        }

        return config;
    }

    @Override
    protected List<EnvVar> getEnvVars(Reconciliation reconciliation) {
        List<EnvVar> varList = new ArrayList<>();
        varList.add(buildEnvVar(ENV_VAR_KAFKA_MIRRORMAKER_CONFIGURATION_CONSUMER,
                getConsumerConfiguration(reconciliation).getConfiguration()));
        varList.add(buildEnvVar(ENV_VAR_KAFKA_MIRRORMAKER_CONFIGURATION_PRODUCER,
                getProducerConfiguration(reconciliation).getConfiguration()));
        varList.add(buildEnvVar(ENV_VAR_KAFKA_MIRRORMAKER_METRICS_ENABLED, String.valueOf(isMetricsEnabled)));
        varList.add(buildEnvVar(ENV_VAR_KAFKA_MIRRORMAKER_BOOTSTRAP_SERVERS_CONSUMER, consumer.getBootstrapServers()));
        varList.add(buildEnvVar(ENV_VAR_KAFKA_MIRRORMAKER_BOOTSTRAP_SERVERS_PRODUCER, producer.getBootstrapServers()));
        varList.add(buildEnvVar(ENV_VAR_KAFKA_MIRRORMAKER_INCLUDE, include));
        varList.add(buildEnvVar(ENV_VAR_KAFKA_MIRRORMAKER_GROUPID_CONSUMER, consumer.getGroupId()));
        if (consumer.getNumStreams() != null) {
            varList.add(buildEnvVar(ENV_VAR_KAFKA_MIRRORMAKER_NUMSTREAMS, Integer.toString(consumer.getNumStreams())));
        }
        if (consumer.getOffsetCommitInterval() != null) {
            varList.add(buildEnvVar(ENV_VAR_KAFKA_MIRRORMAKER_OFFSET_COMMIT_INTERVAL, Integer.toString(consumer.getOffsetCommitInterval())));
        }
        if (producer.getAbortOnSendFailure() != null) {
            varList.add(buildEnvVar(ENV_VAR_KAFKA_MIRRORMAKER_ABORT_ON_SEND_FAILURE, Boolean.toString(producer.getAbortOnSendFailure())));
        }
        varList.add(buildEnvVar(ENV_VAR_STRIMZI_KAFKA_GC_LOG_ENABLED, String.valueOf(gcLoggingEnabled)));
        if (javaSystemProperties != null) {
            varList.add(buildEnvVar(ENV_VAR_STRIMZI_JAVA_SYSTEM_PROPERTIES, ModelUtils.getJavaSystemPropertiesToString(javaSystemProperties)));
        }

        if (tracing != null) {
            varList.add(buildEnvVar(ENV_VAR_STRIMZI_TRACING, tracing.getType()));
        }

        heapOptions(varList, 1.0, 0L);
        jvmPerformanceOptions(varList);

        /** consumer */
        addConsumerEnvVars(varList);

        /** producer */
        addProducerEnvVars(varList);

        varList.add(buildEnvVar(ENV_VAR_STRIMZI_LIVENESS_PERIOD,
                String.valueOf(livenessProbeOptions.getPeriodSeconds() != null ? livenessProbeOptions.getPeriodSeconds() : DEFAULT_HEALTHCHECK_PERIOD)));
        varList.add(buildEnvVar(ENV_VAR_STRIMZI_READINESS_PERIOD,
                String.valueOf(readinessProbeOptions.getPeriodSeconds() != null ? readinessProbeOptions.getPeriodSeconds() : DEFAULT_HEALTHCHECK_PERIOD)));

        // Add shared environment variables used for all containers
        varList.addAll(getRequiredEnvVars());

        addContainerEnvsToExistingEnvs(varList, templateContainerEnvVars);

        return varList;
    }

    /**
     * Sets the consumer related environment variables in the provided List.
     *
     * @param varList   List with environment variables
     */
    private void addConsumerEnvVars(List<EnvVar> varList) {
        if (consumer.getTls() != null) {
            varList.add(buildEnvVar(ENV_VAR_KAFKA_MIRRORMAKER_TLS_CONSUMER, "true"));

            if (consumer.getTls().getTrustedCertificates() != null && consumer.getTls().getTrustedCertificates().size() > 0) {
                StringBuilder sb = new StringBuilder();
                boolean separator = false;
                for (CertSecretSource certSecretSource : consumer.getTls().getTrustedCertificates()) {
                    if (separator) {
                        sb.append(";");
                    }
                    sb.append(certSecretSource.getSecretName() + "/" + certSecretSource.getCertificate());
                    separator = true;
                }
                varList.add(buildEnvVar(ENV_VAR_KAFKA_MIRRORMAKER_TRUSTED_CERTS_CONSUMER, sb.toString()));
            }
        }

        AuthenticationUtils.configureClientAuthenticationEnvVars(consumer.getAuthentication(), varList, name -> ENV_VAR_PREFIX + name + "_CONSUMER");
    }

    /**
     * Sets the producer related environment variables in the provided List.
     *
     * @param varList   List with environment variables
     */
    private void addProducerEnvVars(List<EnvVar> varList) {
        if (producer.getTls() != null) {
            varList.add(buildEnvVar(ENV_VAR_KAFKA_MIRRORMAKER_TLS_PRODUCER, "true"));

            if (producer.getTls().getTrustedCertificates() != null && producer.getTls().getTrustedCertificates().size() > 0) {
                StringBuilder sb = new StringBuilder();
                boolean separator = false;
                for (CertSecretSource certSecretSource : producer.getTls().getTrustedCertificates()) {
                    if (separator) {
                        sb.append(";");
                    }
                    sb.append(certSecretSource.getSecretName() + "/" + certSecretSource.getCertificate());
                    separator = true;
                }
                varList.add(buildEnvVar(ENV_VAR_KAFKA_MIRRORMAKER_TRUSTED_CERTS_PRODUCER, sb.toString()));
            }
        }

        AuthenticationUtils.configureClientAuthenticationEnvVars(producer.getAuthentication(), varList, name -> ENV_VAR_PREFIX + name + "_PRODUCER");
    }

    /**
     * Generates the PodDisruptionBudget.
     *
     * @return The PodDisruptionBudget.
     */
    public PodDisruptionBudget generatePodDisruptionBudget() {
        return createPodDisruptionBudget();
    }

    @Override
    protected String getDefaultLogConfigFileName() {
        return "mirrorMakerDefaultLoggingProperties";
    }

    public void setInclude(String include) {
        this.include = include;
    }

    public void setProducer(KafkaMirrorMakerProducerSpec producer) {
        this.producer = producer;
    }

    public void setConsumer(KafkaMirrorMakerConsumerSpec consumer) {
        this.consumer = consumer;
    }

    protected String getInclude() {
        return include;
    }

    @Override
    protected String getServiceAccountName() {
        return KafkaMirrorMakerResources.serviceAccountName(cluster);
    }

    @Override
    protected boolean shouldPatchLoggerAppender() {
        return true;
    }
}
