/*
 * Copyright 2014-2015 Bud Byrd
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
package com.budjb.rabbitmq.connection

import com.budjb.rabbitmq.ContextState
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import org.apache.log4j.Logger

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ConnectionContextImpl implements ConnectionContext {
    /**
     * Connection configuration.
     */
    ConnectionConfiguration configuration

    /**
     * Lazy-loaded connection to RabbitMQ.
     */
    Connection connection

    /**
     * Connection factory to use to create a connection.
     *
     * This is here for injection during testing.
     */
    ConnectionFactory connectionFactory

    /**
     * Logger.
     */
    private Logger log = Logger.getLogger(ConnectionContextImpl)

    /**
     * Constructor.
     *
     * @param parameters
     */
    ConnectionContextImpl(ConnectionConfiguration configuration) {
        if (configuration == null) {
            throw new NullPointerException("connection configuration can not be null")
        }
        this.configuration = configuration
    }

    /**
     * Opens the connection to the RabbitMQ broker.
     *
     * @throws IllegalStateException
     */
    @Override
    void start() throws IllegalStateException {
        if (this.connection != null) {
            throw new IllegalStateException("attempted to start connection '${getId()}' but it is already started")
        }

        if (!configuration.isValid()) {
            log.error("unable to start connection '${getId()}' because its configuration is invalid")
            return
        }

        log.info("connecting to RabbitMQ server '${getId()}' at '${configuration.getHost()}:${configuration.getPort()}' on virtual host '${configuration.getVirtualHost()}'")

        ConnectionFactory factory = getConnectionFactory()

        factory.setUsername(configuration.getUsername())
        factory.setPassword(configuration.getPassword())
        factory.setPort(configuration.getPort())
        factory.setHost(configuration.getHost())
        factory.setVirtualHost(configuration.getVirtualHost())
        factory.setAutomaticRecoveryEnabled(configuration.getAutomaticReconnect())
        factory.setRequestedHeartbeat(configuration.getRequestedHeartbeat())

        if (configuration.getSsl()) {
            factory.useSslProtocol()
        }

        ExecutorService executorService
        if (configuration.getThreads() > 0) {
            executorService = Executors.newFixedThreadPool(configuration.getThreads())
        }
        else {
            executorService = Executors.newCachedThreadPool()
        }

        this.connection = factory.newConnection(executorService)
    }

    /**
     * Closes the RabbitMQ connection.
     */
    @Override
    void stop() {
        if (!connection?.isOpen()) {
            return
        }

        connection.close()
        connection = null

        log.debug("closed connection to the RabbitMQ server with name '${getId()}'")
    }

    /**
     * Get the context's state.
     *
     * @return
     */
    @Override
    ContextState getState() {
        return this.connection == null ? ContextState.STOPPED : ContextState.STARTED
    }

    /**
     * Returns the name of the connection.
     *
     * @return
     */
    @Override
    String getId() {
        return configuration.name
    }

    /**
     * Creates an un-tracked channel.
     *
     * @return
     */
    @Override
    Channel createChannel() throws IllegalStateException {
        return getConnection().createChannel()
    }

    /**
     * Returns the connection factory to create a connection with.
     *
     * @return
     */
    protected ConnectionFactory getConnectionFactory() {
        if (!connectionFactory) {
            this.connectionFactory = new ConnectionFactory()
        }
        return connectionFactory
    }

    /**
     * Returns the connection's configuration.
     *
     * @return
     */
    @Override
    ConnectionConfiguration getConfiguration() {
        return configuration
    }

    @Override
    Connection getConnection() throws IllegalStateException {
        if (!connection) {
            throw new IllegalStateException("connection '${getId()}' is not active")
        }
        return connection
    }

    /**
     * Returns whether the context is the default connection.
     *
     * @return
     */
    @Override
    boolean getIsDefault() {
        return getConfiguration().isDefault
    }

    /**
     * Sets whether the context is the default connection.
     *
     * @param isDefault
     */
    @Override
    void setIsDefault(boolean isDefault) {
        getConfiguration().isDefault = isDefault
    }
}
