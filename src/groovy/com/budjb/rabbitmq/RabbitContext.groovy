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

import com.budjb.rabbitmq.exception.MissingConfigurationException
import org.apache.log4j.Logger
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClass
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

class RabbitContext {
    /**
     * Connection configuration
     */
    protected ConnectionConfiguration connectionConfiguration

    /**
     * Grails application bean
     */
    GrailsApplication grailsApplication

    /**
     * Logger
     */
    Logger log = Logger.getLogger(this.getClass())

    /**
     * Connection to the RabbitMQ server
     */
    protected Connection connection

    /**
     * List of active channels
     */
    protected List<Channel> channels = []

    /**
     * A list of registered message converters
     */
    public List<MessageConverter> messageConverters = []

    /**
     * A list of services that are set up as consumers
     */
    protected List<Object> consumers = []

    /**
     * Initializes the rabbit driver.
     */
    public void start() {
        // Check for the configuration
        if (!grailsApplication.config.rabbitmq?.connection) {
            if (grailsApplication.config.rabbitmq?.connectionFactory) {
                log.warn("An unsupported legacy config was found. Please refer to the documentation for proper configuration (http://budjb.github.io/grails-rabbitmq-native/doc/manual/)")
            }
            throw new MissingConfigurationException("unable to start application because the RabbitMQ connection configuration was not found")
        }

        // Load the configuration
        connectionConfiguration = new ConnectionConfiguration(grailsApplication.config.rabbitmq.connection)

        // Connect to the server
        connect()
    }

    /**
     * Starts the individual consumers.
     */
    public void startConsumers() {
        consumers.each {
            channels += RabbitConsumer.startConsumer(connection, it)
        }
    }

    /**
     * Reloads the RabbitMQ connection and consumers.
     */
    public void restartConsumers() {
        // Close the existing channels and connection
        stopConsumers()

        // Start the channels again
        startConsumers()
    }

    /**
     * Closes any active channels and the connection to the RabbitMQ server.
     */
    public void stopConsumers() {
        if (channels) {
            log.debug("closing RabbitMQ channels")
            channels.each { channel ->
                if (channel.isOpen()) {
                    channel.close()
                }
            }
            channels = []
        }
        consumers = []
    }

    /**
     * Closes all active channels and disconnects from the RabbitMQ server.
     */
    public void stop() {
        stopConsumers()
        if (connection?.isOpen()) {
            log.debug("closing connection to the RabbitMQ server")
            connection.close()
            connection = null
        }
        messageConverters = []
    }

    /**
     * Disconnects and completely restarts the connection to the RabbitMQ server.
     */
    public void restart() {
        stop()
        start()
        startConsumers()
    }

    /**
     * Creates the connection to the RabbitMQ server.
     */
    protected void connect() {
        // Ensure we don't already have a connection
        if (connection) {
            throw new Exception('will not connect to RabbitMQ; there is already an active connection')
        }

        // Log it
        if (connectionConfiguration.virtualHost) {
            log.info("connecting to RabbitMQ server at '${connectionConfiguration.host}:${connectionConfiguration.port}' on virtual host '${connectionConfiguration.virtualHost}'")
        }
        else {
            log.info("connecting to RabbitMQ server at '${connectionConfiguration.host}:${connectionConfiguration.port}'")
        }

        // Create the connection
        connection = connectionConfiguration.connection
    }

    /**
     * Closes the connection to the RabbitMQ server when the object is destroyed.
     */
    public void finalize() {
        stop()
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
        if (!RabbitConsumer.isConsumer(candidate)) {
            log.warn("not starting '${candidate.shortName}' as a RabbitMQ message consumer because it is not properly configured")
            return false
        }
        consumers << candidate
        return true
    }

    /**
     * Creates a new untracked channel.
     *
     * The caller must make sure to clean this up (channel.close()).
     *
     * @return
     */
    public Channel createChannel() {
        return connection.createChannel()
    }
}
