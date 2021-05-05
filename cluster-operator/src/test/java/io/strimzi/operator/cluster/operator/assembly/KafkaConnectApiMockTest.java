/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.operator.assembly;

import io.strimzi.operator.common.BackOff;
import io.strimzi.test.annotations.ParallelSuite;
import io.strimzi.test.annotations.ParallelTest;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

@ExtendWith(VertxExtension.class)
@ParallelSuite
public class KafkaConnectApiMockTest {
    private static Vertx vertx;
    private BackOff backOff = new BackOff(1L, 2, 3);

    @BeforeAll
    public static void before() {
        vertx = Vertx.vertx();
    }

    @AfterAll
    public static void after() {
        vertx.close();
    }

    @ParallelTest
    public void testStatusWithBackOffSucceedingImmediately(VertxTestContext context) {
        Queue<Future<Map<String, Object>>> statusResults = new ArrayBlockingQueue<>(1);
        statusResults.add(Future.succeededFuture(Collections.emptyMap()));

        KafkaConnectApi api = new MockKafkaConnectApi(vertx, statusResults);
        Checkpoint async = context.checkpoint();

        api.statusWithBackOff(backOff, "some-host", 8083, "some-connector")
            .onComplete(context.succeeding(res -> async.flag()));
    }

    @ParallelTest
    public void testStatusWithBackOffSuccedingEventually(VertxTestContext context) {
        Queue<Future<Map<String, Object>>> statusResults = new ArrayBlockingQueue<>(3);
        statusResults.add(Future.failedFuture(new ConnectRestException(null, null, 404, null, null)));
        statusResults.add(Future.failedFuture(new ConnectRestException(null, null, 404, null, null)));
        statusResults.add(Future.succeededFuture(Collections.emptyMap()));

        KafkaConnectApi api = new MockKafkaConnectApi(vertx, statusResults);
        Checkpoint async = context.checkpoint();

        api.statusWithBackOff(backOff, "some-host", 8083, "some-connector")
            .onComplete(context.succeeding(res -> async.flag()));
    }

    @ParallelTest
    public void testStatusWithBackOffFailingRepeatedly(VertxTestContext context) {
        Queue<Future<Map<String, Object>>> statusResults = new ArrayBlockingQueue<>(4);
        statusResults.add(Future.failedFuture(new ConnectRestException(null, null, 404, null, null)));
        statusResults.add(Future.failedFuture(new ConnectRestException(null, null, 404, null, null)));
        statusResults.add(Future.failedFuture(new ConnectRestException(null, null, 404, null, null)));
        statusResults.add(Future.failedFuture(new ConnectRestException(null, null, 404, null, null)));

        KafkaConnectApi api = new MockKafkaConnectApi(vertx, statusResults);
        Checkpoint async = context.checkpoint();

        api.statusWithBackOff(backOff, "some-host", 8083, "some-connector")
            .onComplete(context.failing(res -> async.flag()));
    }

    @ParallelTest
    public void testStatusWithBackOffOtherExceptionStillFails(VertxTestContext context) {
        Queue<Future<Map<String, Object>>> statusResults = new ArrayBlockingQueue<>(1);
        statusResults.add(Future.failedFuture(new ConnectRestException(null, null, 500, null, null)));

        KafkaConnectApi api = new MockKafkaConnectApi(vertx, statusResults);
        Checkpoint async = context.checkpoint();

        api.statusWithBackOff(backOff, "some-host", 8083, "some-connector")
            .onComplete(context.failing(res -> async.flag()));
    }

    class MockKafkaConnectApi extends KafkaConnectApiImpl   {
        private final Queue<Future<Map<String, Object>>> statusResults;

        public MockKafkaConnectApi(Vertx vertx, Queue<Future<Map<String, Object>>> statusResults) {
            super(vertx);
            this.statusResults = statusResults;
        }

        @Override
        public Future<Map<String, Object>> status(String host, int port, String connectorName) {
            return statusResults.remove();
        }
    }
}
