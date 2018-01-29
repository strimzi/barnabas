/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.strimzi.controller.topic;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Represents a handler for getting Kafka topic metadata, providing a helper {@link #retry} method
 * for subclasses which want to retry when they need to do that
 */
public abstract class TopicMetadataHandler implements Handler<AsyncResult<TopicMetadata>> {

    private static final Logger log = LoggerFactory.getLogger(TopicMetadataHandler.class);

    private final BackOff backOff;

    private final Vertx vertx;
    private final Kafka kafka;
    private final TopicName topicName;

    /**
     * Constructor
     *
     * @param vertx Vert.x instance to use for retrying mechanism
     * @param kafka Kafka client for getting topic metadata
     * @param topicName topic name for which to get metadata
     * @param backOff   backoff information to use for retrying
     */
    TopicMetadataHandler(Vertx vertx, Kafka kafka, TopicName topicName, BackOff backOff) {
        this.vertx = vertx;
        this.kafka = kafka;
        this.topicName = topicName;
        this.backOff = backOff;
    }

    /**
     * Constructor
     *
     * @param vertx Vert.x instance to use for retrying mechanism
     * @param kafka Kafka client for getting topic metadata
     * @param topicName topic name for which to get metadata
     */
    TopicMetadataHandler(Vertx vertx, Kafka kafka, TopicName topicName) {
        this(vertx, kafka, topicName, new BackOff());
    }

    /**
     * Schedules this handler to execute again after a delay defined by the {@code BackOff}.
     * Calls {@link #onMaxAttemptsExceeded} if the backoff has reached its permitted number of retries.
     */
    protected void retry() {

        long delay;
        try {
            delay = backOff.delayMs();
            log.debug("Backing off for {}ms on getting metadata for {}", delay, topicName);
        } catch (MaxAttemptsExceededException e) {
            log.info("Max attempts reached on getting metadata for {} after {}ms, giving up for now", topicName, backOff.totalDelayMs());
            this.onMaxAttemptsExceeded(e);
            return;
        }

        if (delay < 1) {
            // vertx won't tolerate a zero delay
            vertx.runOnContext(timerId -> kafka.topicMetadata(topicName, this));
        } else {
            vertx.setTimer(TimeUnit.MILLISECONDS.convert(delay, TimeUnit.MILLISECONDS),
                    timerId -> kafka.topicMetadata(topicName, this));
        }
    }

    /**
     * Called when the max attempts are exceeded during retry
     *
     * @param e the max attempts exceeded exception instance
     */
    public abstract void onMaxAttemptsExceeded(MaxAttemptsExceededException e);
}
