/*
 * Copyright 2018, Strimzi Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.strimzi.controller.topic;

import io.fabric8.kubernetes.api.model.HasMetadata;

public class InvalidConfigMapException extends ControllerException {
    public InvalidConfigMapException(HasMetadata involvedObject, String message) {
        super(involvedObject, message);
    }
}
