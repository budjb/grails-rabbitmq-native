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
import com.budjb.rabbitmq.connection.ConnectionContext
import com.budjb.rabbitmq.connection.ConnectionManager
import com.budjb.rabbitmq.consumer.ConsumerManager
import com.budjb.rabbitmq.converter.*
import com.budjb.rabbitmq.exception.InvalidConfigurationException
import com.budjb.rabbitmq.exception.MissingConfigurationException

import org.apache.log4j.Logger
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClass
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

class RabbitContextImpl implements RabbitContext, ApplicationContextAware {
    /**
     * Spring application context
     */
    protected ApplicationContext applicationContext

    /**
     * Logger
     */
    protected Logger log = Logger.getLogger(RabbitContextImpl)

    /**
     * The message converter manager.
     */
    protected MessageConverterManager messageConverterManager

    /**
     * The connection context manager.
     */
    protected ConnectionManager connectionManager

    /**
     * Rabbit consumer factory.
     */
    protected ConsumerManager consumerManager

    /**
     * Rabbit queue builder.
     */
    protected QueueBuilder queueBuilder

    /**
     * Creates the exchanges and queues that are defined in the Grails configuration.
     *
     * TODO move this into its own bean
     */
    protected void configureQueues() {
    }

    /**
     * Stars the RabbitMQ system.
     */
    @Override
    public void start() {
        start(false)
    }

    /**
     * Starts the RabbitMQ system.
     */
    @Override
    public void start(boolean skipConsumers) {
        // Start the connections
        connectionManager.open()

        // Set up any configured queues/exchanges
        queueBuilder.configureQueues()

        // Start consumers if requested
        if (!skipConsumers) {
            connectionManager.start()
        }
    }

    /**
     * Starts the individual consumers.
     */
    public void startConsumers() {
        connectionManager.start()
    }

    /**
     * Closes all active channels and disconnects from the RabbitMQ server.
     */
    public void stop() {
        // Reset the connection manager
        connectionManager.reset()

        // Clear message converters
        messageConverterManager.reset()
    }

    /**
     * Disconnects and completely restarts the connection to the RabbitMQ server.
     */
    public void restart() {
        stop()
        load()
        start()
    }

    /**
     * Registers a new message consumer.
     *
     * @param consumer
     * @return
     */
    public void registerConsumer(Object consumer) {
        consumerManager.registerConsumer(consumer)
    }

    /**
     * Creates a new channel with the default connection.
     *
     * Note that this channel must be manually closed.
     *
     * @return
     */
    @Override
    public Channel createChannel() {
        return connectionManager.createChannel()
    }

    /**
     * Creates a new channel with the specified connection.
     *
     * Note that this channel must be manually closed.
     *
     * @return
     */
    @Override
    public Channel createChannel(String connectionName) {
        return connectionManager.createChannel(connectionName)
    }

    /**
     * Returns the ConnectionContext associated with the default connection.
     *
     * @return
     */
    @Override
    public ConnectionContext getConnection() {
        return connectionManager.getConnection()
    }

    /**
     * Returns the ConnectionContext associated with the default connection.
     *
     * @return
     */
    @Override
    public ConnectionContext getConnection(String name) {
        return connectionManager.getConnection(name)
    }

    /**
     * Loads the configuration and registers any consumers and converters.
     */
    @Override
    public void load() {
        // Load the configuration
        connectionManager.load()

        // Load message converters
        messageConverterManager.load()

        // Load consumers
        consumerManager.load()
    }

    /**
     * Registers a message converter with the message converter manager.
     *
     * @param converter
     */
    @Override
    public void registerMessageConverter(MessageConverter converter) {
        messageConverterManager.registerMessageConverter(converter)

    }

    /**
     * Sets the message converter manager.
     */
    @Override
    public void setMessageConverterManager(MessageConverterManager messageConverterManager) {
        this.messageConverterManager = messageConverterManager
    }

    /**
     * Sets the application context bean.
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext
    }

    /**
     * Sets the connection manager.
     */
    @Override
    public void setConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager
    }

    /**
     * Sets the rabbit consumer manager.
     */
    @Override
    public void setConsumerManager(ConsumerManager consumerManager) {
        this.consumerManager = consumerManager
    }

    /**
     * Sets the rabbit queue builder
     */
    @Override
    public void setQueueBuilder(QueueBuilder queueBuilder) {
        this.queueBuilder = queueBuilder
    }
}
