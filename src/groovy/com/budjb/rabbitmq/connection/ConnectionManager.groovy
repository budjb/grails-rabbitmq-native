/*
 * Copyright 2015 Bud Byrd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.budjb.rabbitmq.connection

import groovy.lang.Closure

import org.apache.log4j.Logger
import org.codehaus.groovy.grails.commons.GrailsApplication

import com.budjb.rabbitmq.exception.InvalidConfigurationException
import com.budjb.rabbitmq.exception.MissingConfigurationException
import com.rabbitmq.client.Channel

class ConnectionManager {
    /**
     * Logger.
     */
    Logger log = Logger.getLogger(ConnectionManager)

    /**
     * Registered message converters.
     */
    protected List<ConnectionContext> connections = []

    /**
     * Grails application bean.
     */
    protected GrailsApplication grailsApplication

    /**
     * Loads connections from the configuration.
     */
    public void load() {
        // Grab the configuration
        Object configuration = grailsApplication.config.rabbitmq?.connection

        // Check for the configuration
        if (!configuration) {
            if (grailsApplication.config.rabbitmq?.connectionFactory) {
                log.warn("An unsupported legacy config was found. Please refer to the documentation for proper configuration (http://budjb.github.io/grails-rabbitmq-native/doc/manual/)")
            }
            throw new MissingConfigurationException("unable to start application because the RabbitMQ connection configuration was not found")
        }

        // Make sure we have a supported configuration type
        if (!(configuration instanceof Map || configuration instanceof Closure)) {
            throw new InvalidConfigurationException('RabbitMQ connection configuration is not a Map or a Closure')
        }

        // Load connections
        loadConnections(configuration)

        // Ensure we have at least one connection
        if (connections.size() == 0) {
            throw new InvalidConfigurationException("no RabbitMQ connections were configured")
        }

        // Ensure we don't have more than one default connection
        if (connections.findAll { it.getConfiguration().getIsDefault() == true }.size() > 1) {
            throw new InvalidConfigurationException("more than one default RabbitMQ server connections were configured as default")
        }
    }

    /**
     * Load connections from a Map.
     *
     * @param configuration
     */
    protected void loadConnections(Map configuration) {
        // Create the connection builder
        ConnectionBuilder connectionBuilder = new ConnectionBuilder()

        // Load the connection
        connectionBuilder.connection(configuration)

        // Force it to be default
        connectionBuilder.getConnectionContexts()[0].getConfiguration().setIsDefault(true)

        // Add the connections created by the builder
        connections += connectionBuilder.getConnectionContexts()
    }

    /**
     * Loads connections from a Closure.
     *
     * @param configuration
     */
    protected void loadConnections(Closure configuration) {
        // Create the connection builder
        ConnectionBuilder connectionBuilder = new ConnectionBuilder()

        // Set up and run the closure
        configuration = configuration.clone()
        configuration.delegate = connectionBuilder
        configuration.resolveStrategy = Closure.DELEGATE_FIRST
        configuration()

        // If only one connection was configured, force it as default
        if (connectionBuilder.getConnectionContexts().size() == 1) {
            connectionBuilder.getConnectionContexts()[0].getConfiguration().setIsDefault(true)
        }

        // Add the connections created by the builder
        connections += connectionBuilder.getConnectionContexts()
    }

    /**
     * Opens each context's connection.
     */
    public void open() {
        connections*.openConnection()
    }

    /**
     * Starts the consumers on all connections.
     */
    public void start() {
        connections*.startConsumers()
    }

    /**
     * Stops all consumers, closes all connections, and removes all connection contexts.
     */
    public void reset() {
        connections*.closeConnection()
        connections = []
    }

    /**
     * Returns the default connection.
     *
     * @return
     */
    public ConnectionContext getConnection() {
        ConnectionContext context = connections.find { it.getConfiguration().getIsDefault() == true }

        if (!context) {
            log.error("no default connection found")
        }

        return context
    }

    /**
     * Returns a connection whose name matches the request.
     *
     * @param name
     * @return
     */
    public ConnectionContext getConnection(String name) {
        ConnectionContext context = connections.find { it.getConfiguration().getName() == name }

        if (!context) {
            log.error("no connection with name '${name}' found")
        }

        return context
    }

    /**
     * Builder class for building connection contexts from a configuration file.
     */
    class ConnectionBuilder {
        /**
         * List of connections created by the builder.
         */
        private List<ConnectionContext> connectionContexts = []

        /**
         * Creates a connection context from a configuration or closure method.
         *
         * @param parameters
         * @return
         */
        public void connection(Map parameters) {
            // Build the context
            ConnectionContext context = new ConnectionContext(new ConnectionConfiguration(parameters))

            // Store the context
            connectionContexts << context
        }

        /**
         * Returns the list of connection contexts created by the builder.
         *
         * @return
         */
        public List<ConnectionContext> getConnectionContexts() {
            return connectionContexts
        }
    }

    /**
     * Creates a new channel with the default connection.
     *
     * Note that this channel must be manually closed.
     *
     * @return
     */
    public Channel createChannel() {
        ConnectionContext connection = getConnection()

        if (!connection) {
            throw new Exception('no default connection found')
        }

        return connection.createChannel()
    }

    /**
     * Creates a new channel with the specified connection.
     *
     * Note that this channel must be manually closed.
     *
     * @return
     */
    public Channel createChannel(String connectionName) {
        if (connectionName == null) {
            return createChannel()
        }

        ConnectionContext connection = getConnection(connectionName)

        if (!connection) {
            throw new Exception("no connection with name '${connectionName}' found")
        }

        return connection.createChannel()
    }

    /**
     * Sets the grails application bean.
     *
     * @param grailsApplication
     */
    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
    }
}
