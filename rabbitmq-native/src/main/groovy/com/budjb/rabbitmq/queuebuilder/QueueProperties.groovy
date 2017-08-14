/*
 * Copyright 2017 Bud Byrd
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
package com.budjb.rabbitmq.queuebuilder

import com.budjb.rabbitmq.exception.InvalidConfigurationException

class QueueProperties implements ConfigurationProperties {
    /**
     * Queue arguments (see RabbitMQ documentation).
     */
    Map arguments = [:]

    /**
     * Whether the queue should automatically delete itself once all consumers have disconnected.
     */
    boolean autoDelete = false

    /**
     * Queue binding criteria.
     *
     * This value depends on the type of exchange it is bound to.
     */
    def binding

    /**
     * Whether the queue is durable (messages persisted to disk).
     */
    boolean durable = false

    /**
     * Exchange to bind the queue to. When set, a binding is expected.
     */
    String exchange

    /**
     * Name of the queue.
     */
    String name

    /**
     * Name of the connection the queue should be created with. No value uses the default connection.
     */
    String connection

    /**
     * Header match criteria.
     */
    MatchType match

    /**
     * Whether the queue is exclusive.
     */
    boolean exclusive = false

    /**
     * Constructor.
     *
     * @param name
     * @param properties
     */
    QueueProperties(Map<String, Object> properties) {
        name = parseConfigOption(String, properties.name)
        arguments = parseConfigOption(Map, properties.arguments, arguments)
        autoDelete = parseConfigOption(Boolean, properties.autoDelete, autoDelete)
        binding = parseConfigOption(Object, properties.binding, binding)
        durable = parseConfigOption(Boolean, properties.durable, durable)
        exchange = parseConfigOption(String, properties.exchange, exchange)
        exclusive = parseConfigOption(Boolean, properties.exclusive, exclusive)
        match = MatchType.lookup(parseConfigOption(String, properties.match))
        connection = parseConfigOption(String, properties.connection, connection)
    }

    /**
     * Determines if the minimum requirements of this configuration set have been met and can be considered valid.
     */
    @Override
    void validate() {
        if (!name) {
            throw new InvalidConfigurationException("queue name is required")
        }
        if (binding instanceof Map && match == null) {
            throw new InvalidConfigurationException("Map binding for headers exchanges must have a match type defined")
        }
    }
}
