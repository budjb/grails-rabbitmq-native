/*
 * Copyright 2014 Bud Byrd
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

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

import groovy.util.ConfigObject

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import org.apache.log4j.Logger

@SuppressWarnings("unchecked")
class ConnectionContext {
    /**
     * RabbitMQ host
     */
    protected String host

    /**
     * Name of the connection for use with message consumer configurations.
     */
    protected String name

    /**
     * Whether the connection is the default for message consumers without a named connection.
     */
    protected boolean isDefault = false

    /**
     * RabbitMQ port
     */
    protected int port = ConnectionFactory.DEFAULT_AMQP_PORT

    /**
     * RabbitMQ username
     */
    protected String username

    /**
     * RabbitMQ password
     */
    protected String password

    /**
     * RabbitMQ virtual host
     */
    protected String virtualHost = ConnectionFactory.DEFAULT_VHOST

    /**
     * Whether to automatically reconnect.
     */
    protected boolean automaticReconnect = true

    /**
     * The maximum number of concurrent consumer threads that are processed.
     *
     * 5 is the RabbitMQ default. 0 means unlimited.
     */
    protected int threads = 0

    /**
     * The requested heartbeat delay, in seconds, that the server sends in the connection.tune frame.
     * If set to 0, heartbeats are disabled.
     */
    protected int requestedHeartbeat = ConnectionFactory.DEFAULT_HEARTBEAT

    /**
     * Whether to use SSL.
     */
    protected boolean ssl = false

    /**
     * List of message consumers for this connection.
     */
    protected List<DefaultGrailsMessageConsumerClass> consumers = new ArrayList<DefaultGrailsMessageConsumerClass>()

    /**
     * List of open channels in use by consumers.
     */
    protected List<Channel> channels = new ArrayList<Channel>()

    /**
     * Lazy-loaded connection to RabbitMQ.
     */
    protected Connection connection = null

    /**
     * Logger.
     */
    protected Logger log = Logger.getLogger(getClass())

    /**
     * Constructor.
     *
     * @param parameters
     */
    public ConnectionContext(Map parameters) {
        // Load all parameters
        parameters?.each { key, value -> this[key] = value }

        // Validate the configuration
        validateConfiguration()
    }

    /**
     * Validates that we have the minimum of information needed to connect to RabbitMQ.
     */
    protected void validateConfiguration() {
        assert host != null, "RabbitMQ connection host configuration is missing"
        assert username != null, "RabbitMQ connection username is missing"
        assert password != null, "RabbitMQ connection password is missing"
        assert !virtualHost.isEmpty(), "RabbitMQ connection virtualHost is missing"
        assert port > 0, "RabbitMQ connection port is missing"
        assert threads >= 0, "RabbitMQ connection threads must be greater than or equal to 0"

        if (!name) {
            name = UUID.randomUUID().toString()
            log.warn("connection to RabbitMQ host '${host}:${port}' on virtual host '${virtualHost}' had no name assigned; assigning name '${name}'")
        }
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
        if (virtualHost) {
            log.info("connecting to RabbitMQ server '${name}' at '${host}:${port}' on virtual host '${virtualHost}'")
        }
        else {
            log.info("connecting to RabbitMQ server '${name}' at '${host}:${port}'")
        }

        // Create the connection factory
        ConnectionFactory factory = new ConnectionFactory()

        // Configure it
        factory.username = username
        factory.password = password
        factory.port = port
        factory.host = host
        factory.virtualHost = virtualHost
        factory.automaticRecovery = automaticReconnect
        factory.requestedHeartbeat = requestedHeartbeat

        // Optionally enable SSL
        if (ssl) {
            factory.useSslProtocol()
        }

        // Create the thread pool service
        ExecutorService executorService
        if (threads > 0) {
            executorService = Executors.newFixedThreadPool(threads)
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
        consumers.each {
            channels += RabbitConsumer.startConsumer(this, it)
        }
    }

    /**
     * Stops all consumers associated with this connection.
     */
    public void stopConsumers() {
        if (channels.size()) {
            log.debug("closing RabbitMQ channels")
            channels.each { channel ->
                if (channel.isOpen()) {
                    channel.close()
                }
            }
            channels.clear()
        }
        consumers.clear()
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
    public void registerConsumer(DefaultGrailsMessageConsumerClass clazz) {
        consumers << clazz
    }

    /**
     * Returns the RabbitMQ host.
     */
    public String getHost() {
        return host
    }

    /**
     * Sets the RabbitMQ host.
     */
    public void setHost(String host) {
        this.host = host
    }

    /**
     * Returns the name of the connection.
     */
    public String getName() {
        return name
    }

    /**
     * Sets the name of the connection.
     */
    public void setName(String name) {
        this.name = name
    }

    /**
     * Returns whether the connection is the default connection.
     */
    public boolean getIsDefault() {
        return isDefault
    }

    /**
     * Sets whether the connection is the default connection.
     */
    public void setIsDefault(boolean isDefault) {
        this.isDefault = isDefault
    }

    /**
     * Returns the RabbitMQ broker port.
     */
    public int getPort() {
        return port
    }

    /**
     * Sets the RabbitMQ broker port.
     */
    public void setPort(int port) {
        this.port = port
    }

    /**
     * Returns the RabbitMQ username.
     */
    public String getUsername() {
        return username
    }

    /**
     * Sets the RabbitMQ username.
     */
    public void setUsername(String username) {
        this.username = username
    }

    /**
     * Returns the RabbitMQ password.
     */
    public String getPassword() {
        return password
    }

    /**
     * Sets the RabbitMQ password.
     */
    public void setPassword(String password) {
        this.password = password
    }

    /**
     * Returns the RabbitMQ virtual host.
     */
    public String getVirtualHost() {
        return virtualHost
    }

    /**
     * Sets the RabbitMQ virtual host.
     */
    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost
    }

    /**
     * Returns whether the connection will automatically reconnect.
     */
    public boolean getAutomaticReconnect() {
        return automaticReconnect
    }

    /**
     * Sets whether the connection will automatically reconnect.
     */
    public void setAutomaticReconnect(boolean automaticReconnect) {
        this.automaticReconnect = automaticReconnect
    }

    /**
     * Returns the maximum number of concurrent consumer threads that are processed.
     *
     * 5 is the RabbitMQ default. 0 means unlimited.
     */
    public int getThreads() {
        return threads
    }

    /**
     * Sets the maximum number of concurrent consumer threads that are processed.
     *
     * 5 is the RabbitMQ default. 0 means unlimited.
     */
    public void setThreads(int threads) {
        this.threads = threads
    }

    /**
     * Returns the requested heartbeat delay, in seconds, that the server sends in the connection.tune frame.
     *
     * If set to 0, heartbeats are disabled.
     */
    public int getRequestedHeartbeat() {
        return requestedHeartbeat
    }

    /**
     * Sets the requested heartbeat delay, in seconds, that the server sends in the connection.tune frame.
     *
     * If set to 0, heartbeats are disabled.
     */
    public void setRequestedHeartbeat(int requestedHeartbeat) {
        this.requestedHeartbeat = requestedHeartbeat
    }

    /**
     * Returns whether to use SSL.
     */
    public boolean getSsl() {
        return ssl
    }

    /**
     * Sets whether to use SSL.
     */
    public void setSsl(boolean ssl) {
        this.ssl = ssl
    }
}
