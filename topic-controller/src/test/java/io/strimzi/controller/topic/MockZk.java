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

import io.strimzi.controller.topic.zk.Zk;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.util.List;

class MockZk implements Zk {

    public AsyncResult<Zk> connectResult = Future.failedFuture("Unexpected mock interaction. Configure " + getClass().getSimpleName()+".connectResult");
    public AsyncResult<Void> createResult = Future.failedFuture("Unexpected mock interaction. Configure " + getClass().getSimpleName()+".connectResult");
    public AsyncResult<Void> setDataResult = Future.failedFuture("Unexpected mock interaction. Configure " + getClass().getSimpleName()+".setDataResult");
    public AsyncResult<List<String>> childrenResult = Future.failedFuture("Unexpected mock interaction. Configure " + getClass().getSimpleName()+".childrenResult");
    public AsyncResult<byte[]> dataResult = Future.failedFuture("Unexpected mock interaction. Configure " + getClass().getSimpleName()+".dataResult");
    private Handler<AsyncResult<List<String>>> childrenHandler;

    public void triggerChildren(AsyncResult<List<String>> childrenResult) {
        childrenHandler.handle(childrenResult);
    }

    @Override
    public Zk connect(Handler<AsyncResult<Zk>> handler) {
        handler.handle(connectResult);
        return this;
    }

    @Override
    public Zk temporaryConnectionHandler(Handler<AsyncResult<ZooKeeper>> handler) {
        return this;
    }

    @Override
    public Zk disconnectionHandler(Handler<AsyncResult<Zk>> handler) {
        return this;
    }

    @Override
    public Zk disconnect(Handler<AsyncResult<Void>> handler) {
        return this;
    }

    @Override
    public Zk create(String path, byte[] data, List<ACL> acls, CreateMode createMode, Handler<AsyncResult<Void>> handler) {
        handler.handle(createResult);
        return this;
    }

    @Override
    public Zk setData(String path, byte[] data, int version, Handler<AsyncResult<Void>> handler) {
        handler.handle(setDataResult);
        return this;
    }

    @Override
    public Zk children(String path, boolean watch, Handler<AsyncResult<List<String>>> handler) {
        childrenHandler = handler;
        handler.handle(childrenResult);
        return this;
    }

    @Override
    public Zk setData(String path, boolean watch, Handler<AsyncResult<byte[]>> handler) {
        handler.handle(dataResult);
        return this;
    }

    @Override
    public Zk delete(String path, int version, Handler<AsyncResult<Void>> handler) {
        return null;
    }

    @Override
    public Zk watchExists(String path, Handler<AsyncResult<Stat>> watcher, Handler<AsyncResult<Stat>> complete) {
        return null;
    }

    @Override
    public Zk unwatchExists(String path, Handler<AsyncResult<Stat>> watcher, Handler<AsyncResult<Void>> complete) {
        return null;
    }

    @Override
    public Zk exists(String path, Handler<AsyncResult<Stat>> handler) {
        return null;
    }
}
