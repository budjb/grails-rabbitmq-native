/*
 * Copyright 2015 Bud Byrd
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
package com.budjb.rabbitmq

import com.budjb.rabbitmq.connection.ConnectionConfiguration
import com.budjb.rabbitmq.connection.ConnectionManager
import com.budjb.rabbitmq.consumer.ConsumerManager
import com.budjb.rabbitmq.consumer.MessageConsumer
import com.budjb.rabbitmq.converter.MessageConverter
import com.budjb.rabbitmq.converter.MessageConverterManager
import com.budjb.rabbitmq.report.ConnectionReport
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection

interface RabbitContext {
    /**
     * Loads the configuration and registers any consumers or converters.
     */
    void load()

    /**
     * Starts all connections, creates exchanges and queues, and starts all consumers.
     */
    void start()

    /**
     * Starts all connections and creates exchanges and queues.  Optionally starts
     * consumers.
     *
     * This method is useful in situation where the system needs to be started up
     * but other work needs to be done before messages can start consuming.
     *
     * @param deferConsumers
     */
    void start(boolean deferConsumers)

    /**
     * Stops all consumers and connections.
     */
    void stop()

    /**
     * Stops all consumers and connections, reloads the application configurations,
     * and finally starts all connections and consumers.
     *
     * Calling this method will retain any manually registered consumers and connections
     * unless they are overridden by the configuration.
     */
    void reload()

    /**
     * Stops all consumers and connections, and removes any registered contexts.
     */
    void reset()

    /**
     * Starts all consumers.
     */
    void startConsumers()

    /**
     * Starts all consumers associated with the given connection name.
     */
    void startConsumers(String connectionName)

    /**
     * Starts a consumer based on its name.
     */
    void startConsumer(String name)

    /**
     * Stops all consumers.
     */
    void stopConsumers()

    /**
     * Stops all consumers associated with the given connection name.
     */
    void stopConsumers(String connectionName)

    /**
     * Stops a consumer based on its name.
     *
     * @param name
     */
    void stopConsumer(String name)

    /**
     * Registers a consumer.
     *
     * @param consumer
     */
    void registerConsumer(MessageConsumer consumer)

    /**
     * Registers a message converter.
     *
     * @param converter
     */
    void registerMessageConverter(MessageConverter converter)

    /**
     * Starts all connections.
     */
    void startConnections()

    /**
     * Starts the connection with the given name.
     *
     * @param name
     */
    void startConnection(String name)

    /**
     * Stops all connections.
     *
     * This will also stop all consumers.
     */
    void stopConnections()

    /**
     * Stops the connection with the given name.
     *
     * This will also stop all consumers using the connection.
     *
     * @param name
     */
    void stopConnection(String name)

    /**
     * Registers a new connection.
     *
     * @param configuration
     */
    void registerConnection(ConnectionConfiguration configuration)

    /**
     * Creates a RabbitMQ Channel with the default connection.
     *
     * @return
     */
    Channel createChannel()

    /**
     * Creates a RabbitMQ Channel with the specified connection.
     *
     * @return
     */
    Channel createChannel(String connectionName)

    /**
     * Returns the RabbitMQ Connection associated with the default connection.
     *
     * @return
     */
    Connection getConnection()

    /**
     * Returns the RabbitMQ Connection with the specified connection name.
     *
     * @param name
     * @return
     */
    Connection getConnection(String name)

    /**
     * Sets the message converter manager.
     *
     * @param messageConverterManager
     */
    void setMessageConverterManager(MessageConverterManager messageConverterManager)

    /**
     * Sets the connection manager.
     */
    void setConnectionManager(ConnectionManager connectionManager)

    /**
     * Sets the rabbit consumer manager.
     */
    void setConsumerManager(ConsumerManager consumerManager)

    /**
     * Sets the rabbit queue builder.
     */
    void setQueueBuilder(QueueBuilder queueBuilder)

    /**
     * Creates any configured exchanges and queues.
     */
    void createExchangesAndQueues()

    /**
     * Get the overall running state of consumers and connections.
     *
     * @return
     */
    RunningState getRunningState()

    /**
     * Perform a graceful shutdown of consumers and then disconnect.
     *
     * This method blocks until the full shutdown is complete.
     */
    void shutdown()

    /**
     * Generates a report about all connections and consumers.
     *
     * @return
     */
    List<ConnectionReport> getStatusReport()
}
