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

import com.budjb.rabbitmq.consumer.ConsumerAdapter
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import org.apache.log4j.Logger

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@SuppressWarnings("unchecked")
class ConnectionContext {
    /**
     * List of message consumers for this connection.
     */
    protected List<ConsumerAdapter> adapters = []

    /**
     * Connection configuration.
     */
    ConnectionConfiguration configuration

    /**
     * Lazy-loaded connection to RabbitMQ.
     */
    protected Connection connection

    /**
     * Connection factory to use to create a connection.
     *
     * This is here for injection during testing.
     */
    protected ConnectionFactory connectionFactory

    /**
     * Logger.
     */
    protected Logger log = Logger.getLogger(ConnectionContext)

    /**
     * Constructor.
     *
     * @param parameters
     */
    public ConnectionContext(ConnectionConfiguration configuration) {
        this.configuration = configuration
    }

    /**
     * Returns a connection instance based on this context's configuration properties.
     *
     * @return
     */
    public Connection getConnection() {
        // Connect if we are not already connected
        if (!this.connection) {
            openConnection()
        }

        return this.connection
    }

    /**
     * Starts the connection to the RabbitMQ broker.
     */
    public void openConnection() {
        // Log it
        log.info("connecting to RabbitMQ server '${configuration.getName()}' at '${configuration.getHost()}:${configuration.getPort()}' on virtual host '${configuration.getVirtualHost()}'")

        // Create the connection factory
        ConnectionFactory factory = getConnectionFactory()

        // Configure it
        factory.setUsername(configuration.getUsername())
        factory.setPassword(configuration.getPassword())
        factory.setPort(configuration.getPort())
        factory.setHost(configuration.getHost())
        factory.setVirtualHost(configuration.getVirtualHost())
        factory.setAutomaticRecoveryEnabled(configuration.getAutomaticReconnect())
        factory.setRequestedHeartbeat(configuration.getRequestedHeartbeat())

        // Optionally enable SSL
        if (configuration.getSsl()) {
            factory.useSslProtocol()
        }

        // Create the thread pool service
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
     * Closes the connection to the RabbitMQ broker, if it's open.
     */
    public void closeConnection() {
        if (!connection?.isOpen()) {
            return
        }

        stopConsumers()

        log.debug("closing connection to the RabbitMQ server")
        connection.close()
        connection = null
    }

    /**
     * Starts all consumers associated with this connection.
     */
    public void startConsumers() {
        adapters*.start()
    }

    /**
     * Stops all consumers associated with this connection.
     */
    public void stopConsumers() {
        adapters*.stop()
    }

    /**
     * Creates an un-tracked channel.
     *
     * @return
     */
    public Channel createChannel() {
        return connection.createChannel()
    }

    /**
     * Adds a consumer to the connection.
     *
     * @param clazz
     */
    public void registerConsumer(ConsumerAdapter adapter) {
        adapters << adapter
    }

    /**
     * Sets the connection factory to create a connection with.
     *
     * @param connectionFactory
     */
    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory
    }

    /**
     * Returns the connection factory to create a connection with.
     * @return
     */
    protected ConnectionFactory getConnectionFactory() {
        if (!connectionFactory) {
            this.connectionFactory = new ConnectionFactory()
        }
        return connectionFactory
    }
}
