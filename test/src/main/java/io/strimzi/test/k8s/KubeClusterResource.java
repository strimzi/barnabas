/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.test.k8s;

import io.strimzi.test.k8s.cluster.KubeCluster;
import io.strimzi.test.k8s.cmdClient.KubeCmdClient;
import io.strimzi.test.k8s.exceptions.NoClusterException;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * A Junit resource which discovers the running cluster and provides an appropriate KubeClient for it,
 * for use with {@code @BeforeAll} (or {@code BeforeEach}.
 * For example:
 * <pre><code>
 *     public static KubeClusterResource testCluster = new KubeClusterResources();
 *
 *     &#64;BeforeEach
 *     void before() {
 *         testCluster.before();
 *     }
 * </code></pre>
 */
public class KubeClusterResource {

    private final boolean bootstrap;
    private KubeCluster cluster;
    private KubeCmdClient cmdClient;
    private KubeClient client;
    private HelmClient helmClient;
    private static KubeClusterResource instance;

    public static synchronized KubeClusterResource getInstance() {
        if (instance == null) {
            instance = new KubeClusterResource();
        }
        return instance;
    }

    private KubeClusterResource() {
        bootstrap = true;
    }

    /** Gets the namespace in use */
    public String defaultNamespace() {
        return cmdClient().defaultNamespace();
    }

    public KubeCmdClient cmdClient() {
        if (cmdClient == null) {
            this.cmdClient = cluster().defaultCmdClient();
        }
        return cmdClient;
    }

    public KubeClient client() {
        if (client == null) {
            this.client = cluster().defaultClient();
        }
        return client;
    }

    public HelmClient helmClient() {
        if (helmClient == null) {
            this.helmClient = HelmClient.findClient(cmdClient());
        }
        return helmClient;
    }

    public KubeCluster cluster() {
        if (cluster == null) {
            try {
                this.cluster = KubeCluster.bootstrap();
            } catch (NoClusterException e) {
                assumeTrue(false, e.getMessage());
            }
        }
        return cluster;
    }

    public void before() {
        if (bootstrap) {
            if (cluster == null) {
                try {
                    this.cluster = KubeCluster.bootstrap();
                } catch (NoClusterException e) {
                    assumeTrue(false, e.getMessage());
                }
            }
            if (cmdClient == null) {
                this.cmdClient = cluster.defaultCmdClient();
            }
            if (client == null) {
                this.client = cluster.defaultClient();
            }
        }
    }
}
