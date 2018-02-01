package io.strimzi.controller.cluster.operations.kubernetes;

import io.fabric8.kubernetes.client.dsl.ScalableResource;
import io.strimzi.controller.cluster.K8SUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScaleUpOperation {
    private static final Logger log = LoggerFactory.getLogger(ScaleUpOperation.class.getName());
    private final Vertx vertx;
    private final K8SUtils k8s;

    public ScaleUpOperation(Vertx vertx, K8SUtils k8s) {
        this.vertx = vertx;
        this.k8s = k8s;
    }

    public void scaleUp(ScalableResource resource, int scaleTo, Handler<AsyncResult<Void>> handler) {
        vertx.createSharedWorkerExecutor("kubernetes-ops-pool").executeBlocking(
                future -> {
                    try {
                        log.info("Scaling up to {} replicas", scaleTo);
                        k8s.scale(resource, scaleTo, true);
                        future.complete();
                    }
                    catch (Exception e) {
                        log.error("Caught exception while scaling up", e);
                        future.fail(e);
                    }
                },
                false,
                res -> {
                    if (res.succeeded()) {
                        log.info("Scaling up to {} replicas has been completed", scaleTo);
                        handler.handle(Future.succeededFuture());
                    }
                    else {
                        log.error("Scaling up has failed: {}", res.cause().toString());
                        handler.handle(Future.failedFuture(res.cause()));
                    }
                }
        );
    }
}
