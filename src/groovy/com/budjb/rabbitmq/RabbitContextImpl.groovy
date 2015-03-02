/*
 * Copyright 2013-2015 Bud Byrd
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
import com.budjb.rabbitmq.converter.MessageConverter
import com.budjb.rabbitmq.converter.MessageConverterManager
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection

class RabbitContextImpl implements RabbitContext {
    /**
     * Message converter manager.
     */
    MessageConverterManager messageConverterManager

    /**
     * Connection manager.
     */
    ConnectionManager connectionManager

    /**
     * Consumer manager.
     */
    ConsumerManager consumerManager

    /**
     * Queue builder.
     */
    QueueBuilder queueBuilder

    /**
     * Loads the configuration and registers any consumers or converters.
     */
    @Override
    void load() {
        messageConverterManager.load()
        connectionManager.load()
        consumerManager.load()
    }

    /**
     * Starts all connections, creates exchanges and queues, and starts all consumers.
     */
    @Override
    void start() {
        startConnections()
        createExchangesAndQueues()
        startConsumers()
    }

    /**
     * Starts all connections and creates exchanges and queues.  Optionally starts
     * consumers.
     *
     * This method is useful in situation where the system needs to be started up
     * but other work needs to be done before messages can start consuming.
     *
     * @param deferConsumers
     */
    @Override
    void start(boolean deferConsumers) {
        startConnections()
        createExchangesAndQueues()
        if (!deferConsumers) {
            startConsumers()
        }
    }

    /**
     * Stops all consumers and connections.
     */
    @Override
    void stop() {
        stopConnections() // this will also stop consumers
    }

    /**
     * Stops all consumers and connections, reloads the application configurations,
     * and finally starts all connections and consumers.
     *
     * Calling this method will retain any manually registered consumers and connections
     * unless they are overridden by the configuration.
     */
    @Override
    void reload() {
        stop()
        load()
        start()
    }

    /**
     * Stops all consumers and connections, and removes any registered contexts.
     */
    @Override
    void reset() {
        stop()
        messageConverterManager.reset()
        consumerManager.reset()
        connectionManager.reset()
    }

    /**
     * Starts all consumers.
     */
    @Override
    void startConsumers() {
        consumerManager.start()
    }

    /**
     * Starts all consumers associated with the given connection name.
     */
    @Override
    void startConsumers(String connectionName) {
        consumerManager.start(connectionManager.getContext(connectionName))
    }

    /**
     * Starts a consumer based on its name.
     */
    @Override
    void startConsumer(String name) {
        consumerManager.start(name)
    }

    /**
     * Stops all consumers.
     */
    @Override
    void stopConsumers() {
        consumerManager.stop()
    }

    /**
     * Stops all consumers associated with the given connection name.
     */
    @Override
    void stopConsumers(String connectionName) {
        consumerManager.stop(connectionManager.getContext(connectionName))
    }

    /**
     * Stops a consumer based on its name.
     *
     * @param name
     */
    @Override
    void stopConsumer(String name) {
        consumerManager.stop(name)
    }

    /**
     * Registers a consumer.
     *
     * @param candidate
     */
    @Override
    void registerConsumer(Object consumer) {
        consumerManager.register(consumerManager.createContext(consumer))
    }

    /**
     * Registers a message converter.
     *
     * @param converter
     */
    @Override
    void registerMessageConverter(MessageConverter converter) {
        messageConverterManager.registerMessageConverter(converter)
    }

    /**
     * Starts all connections.
     */
    @Override
    void startConnections() {
        connectionManager.start()
    }

    /**
     * Starts the connection with the given name.
     *
     * @param name
     */
    @Override
    void startConnection(String name) {
        connectionManager.start(name)
    }

    /**
     * Stops all connections.
     *
     * This will also stop all consumers.
     */
    @Override
    void stopConnections() {
        stopConsumers()
        connectionManager.stop()
    }

    /**
     * Stops the connection with the given name.
     *
     * This will also stop all consumers using the connection.
     *
     * @param name
     */
    @Override
    void stopConnection(String name) {
        stopConsumers(name)
        connectionManager.stop(name)
    }

    /**
     * Registers a new connection.
     *
     * @param configuration
     */
    @Override
    void registerConnection(ConnectionConfiguration configuration) {
        connectionManager.register(connectionManager.createContext(configuration))
    }

    /**
     * Creates a RabbitMQ Channel with the default connection.
     *
     * @return
     */
    @Override
    Channel createChannel() {
        return connectionManager.createChannel()
    }

    /**
     * Creates a RabbitMQ Channel with the specified connection.
     *
     * @return
     */
    @Override
    Channel createChannel(String connectionName) {
        return connectionManager.createChannel(connectionName)
    }

    /**
     * Returns the RabbitMQ Connection associated with the default connection.
     *
     * @return
     */
    @Override
    Connection getConnection() {
        return connectionManager.getConnection()
    }

    /**
     * Returns the RabbitMQ Connection with the specified connection name.
     *
     * @param name
     * @return
     */
    @Override
    Connection getConnection(String name) {
        return connectionManager.getConnection(name)
    }

    /**
     * Creates any configured exchanges and queues.
     */
    @Override
    void createExchangesAndQueues() {
        queueBuilder.configureQueues()
    }
}
