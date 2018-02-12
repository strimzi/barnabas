/*
 * Copyright 2017-2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.controller.cluster.operations.cluster;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.strimzi.controller.cluster.ClusterController;
import io.strimzi.controller.cluster.resources.AbstractCluster;
import io.strimzi.controller.cluster.resources.ClusterDiffResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.shareddata.Lock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract cluster creation, update, read, delection, etc, for a generic cluster type {@code C}.
 * This class applies the template method pattern, first obtaining the desired cluster configuration
 * ({@link CompositeOperation#getCluster(String, String)}),
 * then creating resources to match ({@link CompositeOperation#composite(String, ClusterOperation)}.
 *
 * This class manages a per-cluster-type and per-cluster locking strategy so only one operation per cluster
 * can proceed at once.
 * @param <C> The type of Kubernetes client
 */
public abstract class AbstractClusterOperations<C extends AbstractCluster,
        R extends HasMetadata> {

    private static final Logger log = LoggerFactory.getLogger(AbstractClusterOperations.class.getName());

    protected static final String OP_CREATE = "create";
    protected static final String OP_DELETE = "delete";
    protected static final String OP_UPDATE = "update";

    protected final int LOCK_TIMEOUT = 60000;

    protected final Vertx vertx;
    protected final boolean isOpenShift;

    protected AbstractClusterOperations(Vertx vertx, boolean isOpenShift) {
        this.vertx = vertx;
        this.isOpenShift = isOpenShift;
    }

    protected final String getLockName(String clusterType, String namespace, String name) {
        return "lock::"+ clusterType +"::" + namespace + "::" + name;
    }

    protected static class ClusterOperation<C extends AbstractCluster> {
        private final C cluster;
        private final ClusterDiffResult diff;

        public ClusterOperation(C cluster, ClusterDiffResult diff) {
            this.cluster = cluster;
            this.diff = diff;
        }

        public C cluster() {
            return cluster;
        }

        public ClusterDiffResult diff() {
            return diff;
        }

    }

    protected interface CompositeOperation<C extends AbstractCluster> {
        /**
         * Create the resources in Kubernetes according to the given {@code cluster},
         * returning a composite future for when the overall operation is done
         */
        Future<?> composite(String namespace, ClusterOperation<C> operation);

        /** Get the desired Cluster instance */
        ClusterOperation<C> getCluster(String namespace, String name);
    }

    protected final <C extends AbstractCluster> void execute(String clusterType, String operationType, String namespace, String name, CompositeOperation<C> compositeOperation, Handler<AsyncResult<Void>> handler) {
        final String lockName = getLockName(clusterType, namespace, name);
        vertx.sharedData().getLockWithTimeout(lockName, LOCK_TIMEOUT, res -> {
            if (res.succeeded()) {
                Lock lock = res.result();

                ClusterOperation<C> clusterOp;
                try {
                    clusterOp = compositeOperation.getCluster(namespace, name);
                    log.info("{} {} cluster {} in namespace {}", operationType, clusterType, clusterOp.cluster().getName(), namespace);
                } catch (Exception ex) {
                    log.error("Error while getting required {} cluster state for {} operation", clusterType, operationType, ex);
                    handler.handle(Future.failedFuture("getCluster error"));
                    lock.release();
                    return;
                }
                Future<?> composite = compositeOperation.composite(namespace, clusterOp);

                composite.setHandler(ar -> {
                    if (ar.succeeded()) {
                        log.info("{} cluster {} in namespace {}: successful {}", clusterType, clusterOp.cluster().getName(), namespace, operationType);
                        handler.handle(Future.succeededFuture());
                        lock.release();
                    } else {
                        log.error("{} cluster {} in namespace {}: failed to {}", clusterType, clusterOp.cluster().getName(), namespace, operationType);
                        handler.handle(Future.failedFuture("Failed to execute cluster operation"));
                        lock.release();
                    }
                });
            } else {
                log.error("Failed to acquire lock to {} {} cluster {}", operationType, clusterType, lockName);
                handler.handle(Future.failedFuture("Failed to acquire lock to " + operationType + " "+ clusterType + " cluster"));
            }
        });
    }

    protected abstract void create(String namespace, String name, Handler<AsyncResult<Void>> handler);

    public void createByName(String namespace, ConfigMap add)   {
        String name = add.getMetadata().getName();
        log.info("Adding cluster {}", name);

        create(namespace, name, res -> {
            if (res.succeeded()) {
                log.info("Kafka cluster added {}", name);
            }
            else {
                log.error("Failed to add Kafka cluster {}.", name);
            }
        });
    }

    protected abstract void delete(String namespace, String name, Handler<AsyncResult<Void>> handler);

    //@Override
    public void deleteByLabel(String namespace, R dep)   {
        String name = dep.getMetadata().getLabels().get(ClusterController.STRIMZI_CLUSTER_LABEL);
        log.info("Deleting cluster {} in namespace {}", name, namespace);
        delete(namespace, name);
    }

    //@Override
    public void deleteByName(String namespace, ConfigMap cm)   {
        String name = cm.getMetadata().getName();
        log.info("Deleting cluster {} in namespace {}", name, namespace);
        delete(namespace, name);
    }

    private void delete(String namespace, String name)   {
        delete(namespace, name, res -> {
            if (res.succeeded()) {
                log.info("cluster deleted {} in namespace {}", name, namespace);
            }
            else {
                log.error("Failed to delete cluster {} in namespace {}", name, namespace);
            }
        });
    }

    protected abstract void update(String namespace, String name, Handler<AsyncResult<Void>> handler);

    public void updateByName(String namespace, ConfigMap cm)   {
        String name = cm.getMetadata().getName();
        log.info("Checking for updates in cluster {}", cm.getMetadata().getName());

        update(namespace, name, res2 -> {
            if (res2.succeeded()) {
                log.info("Kafka cluster updated {}", name);
            }
            else {
                log.error("Failed to update Kafka cluster {}.", name);
            }
        });
    }

}
