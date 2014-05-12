/*
 * Copyright 2013 Bud Byrd
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

import com.budjb.rabbitmq.exception.InvalidConfigurationException
import com.budjb.rabbitmq.exception.MissingConfigurationException
import org.apache.log4j.Logger
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClass
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

class RabbitContext {
    /**
     * Grails application bean
     */
    GrailsApplication grailsApplication

    /**
     * Logger
     */
    Logger log = Logger.getLogger(this.getClass())

    /**
     * List of current connection contexts.
     */
    protected List<ConnectionContext> connections = []

    /**
     * A list of registered message converters.
     */
    public List<MessageConverter> messageConverters = []

    /**
     * Loads and initializes the configuration.
     */
    public void loadConfiguration() {
        // Check for the configuration
        if (!grailsApplication.config.rabbitmq?.connection) {
            if (grailsApplication.config.rabbitmq?.connectionFactory) {
                log.warn("An unsupported legacy config was found. Please refer to the documentation for proper configuration (http://budjb.github.io/grails-rabbitmq-native/doc/manual/)")
            }
            throw new MissingConfigurationException("unable to start application because the RabbitMQ connection configuration was not found")
        }

        // Load the configuration
        connections = ConnectionBuilder.loadConnections(grailsApplication.config.rabbitmq.connection)

        // Ensure we have at least one connection
        if (connections.size() == 0) {
            throw new InvalidConfigurationException("no RabbitMQ connections were configured")
        }

        // Ensure we don't have more than one default connection
        if (connections.findAll { it.isDefault == true }.size() > 1) {
            throw new InvalidConfigurationException("more than one default RabbitMQ server connections were configured as default")
        }
    }

    /**
     * Connects to each configured RabbitMQ broker.
     */
    public void start() {
        connections*.openConnection()
    }

    /**
     * Starts the individual consumers.
     */
    public void startConsumers() {
        connections*.startConsumers()
    }

    /**
     * Reloads message consumers, but leaves the connections intact.
     */
    public void restartConsumers() {
        // Close the existing channels
        stopConsumers()

        // Start the channels again
        startConsumers()
    }

    /**
     * Closes any active channels and the connection to the RabbitMQ server.
     */
    public void stopConsumers() {
        connections*.stopConsumers()
    }

    /**
     * Closes all active channels and disconnects from the RabbitMQ server.
     */
    public void stop() {
        // Stop consumers
        stopConsumers()

        // Disconnect
        connections*.closeConnection()
        connections = []

        // Clear message converters
        messageConverters = []
    }

    /**
     * Disconnects and completely restarts the connection to the RabbitMQ server.
     */
    public void restart() {
        stop()
        loadConfiguration()
        start()
        startConsumers()
    }

    /**
     * Creates the connection to the RabbitMQ server.
     */
    protected void connect() {
        connections*.openConnection()
    }

    /**
     * Registers a message converter against the rabbit context.
     *
     * @param converter
     */
    public void registerMessageConverter(MessageConverter converter) {
        log.debug("registering message converter '${converter.class.simpleName}' for type '${converter.type}'")
        messageConverters << converter
    }

    /**
     * Attempts to register a grails class as a consumer.
     *
     * @param candidate
     * @return
     */
    public boolean registerConsumer(DefaultGrailsMessageConsumerClass candidate) {
        // Validate the consumer configuration
        if (!RabbitConsumer.isConsumer(candidate)) {
            log.warn("not starting '${candidate.shortName}' as a RabbitMQ message consumer because it is not properly configured")
            return false
        }

        // Get the proper connection
        ConnectionContext connection = getConnection(RabbitConsumer.getConnectionName(candidate))

        // If the connection wasn't found, bail out
        if (!connection) {
            return false
        }

        // Add the consumer to the connection
        connection.registerConsumer(candidate)

        return true
    }

    /**
     * Creates a new untracked channel.
     *
     * The caller must make sure to clean this up (channel.close()).
     *
     * @return
     */
    public Channel createChannel(String connectionName = null) {
        // Get the requested connection
        ConnectionContext connection = getConnection(connectionName)

        if (!connection) {
            if (!connectionName) {
                throw new Exception("no default connection found")
            }
            else {
                throw new Exception("no connection with name '${connectionName}' found")
            }
        }

        return connection.createChannel()
    }

    /**
     * Returns a connection by its name, or the default if the name is null.
     *
     * @return
     */
    public ConnectionContext getConnection(String name = null) {
        ConnectionContext context

        if (!name) {
            context = connections.find { it.isDefault == true }

            if (!context) {
                log.error("no default connection found")
                return null
            }
        }
        else {
            context = connections.find { it.name == name }

            if (!context) {
                log.error("no connection with name '${name}' found")
            }
        }

        return context
    }
}
