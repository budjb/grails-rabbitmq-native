/*
 * Copyright 2013-2017 Bud Byrd
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
package com.budjb.rabbitmq.exception

/**
 * An exception for consumers that have handlers defined more than once for the same object type.
 */
class DuplicateHandlerException extends RuntimeException {
    /**
     * Duplicated class type.
     */
    final Class<?> type

    /**
     * Constructor.
     *
     * @param type Duplicated class type.
     */
    DuplicateHandlerException(Class<?> type) {
        super("multiple message handlers that support type ${type.getName()} were found in a single message consumer")
        this.type = type
    }
}
