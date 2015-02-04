/*
 * Copyright 2013-2014 Bud Byrd
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

import java.util.List

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
     * Grails application bean
     */
    GrailsApplication grailsApplication

    /**
     * Spring application context
     */
    ApplicationContext applicationContext

    /**
     * Logger
     */
    Logger log = Logger.getLogger(this.getClass())

    /**
     * List of current connection contexts.
     */
    protected List<ConnectionContext> connections = new ArrayList<ConnectionContext>()

    /**
     * The message converter manager.
     */
    protected MessageConverterManager messageConverterManager

    /**
     * Loads and initializes the configuration.
     */
    protected void loadConfiguration() {
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
    @Override
    public void start() {
        start(false)
    }

    @Override
    public void start(boolean skipConsumers) {
        connections*.openConnection()
        configureQueues()
        if (!skipConsumers) {
            startConsumers()
        }
    }

    /**
     * Creates the exchanges and queues that are defined in the Grails configuration.
     */
    protected void configureQueues() {
        // Skip if the config isn't defined
        if (!(grailsApplication.config.rabbitmq?.queues instanceof Closure)) {
            return
        }

        // Grab the config closure
        Closure config = grailsApplication.config.rabbitmq.queues

        // Create the queue builder
        RabbitQueueBuilder queueBuilder = new RabbitQueueBuilder(this)

        // Run the config
        config = config.clone()
        config.delegate = queueBuilder
        config.resolveStrategy = Closure.DELEGATE_FIRST
        config()
    }

    /**
     * Starts the individual consumers.
     */
    public void startConsumers() {
        connections*.startConsumers()
    }

    /**
     * Closes any active channels and the connection to the RabbitMQ server.
     */
    protected void stopConsumers() {
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
        connections.clear()

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
     * Creates the connection to the RabbitMQ server.
     */
    protected void connect() {
        connections*.openConnection()
    }

    /**
     * Attempts to register a grails class as a consumer.
     *
     * @param candidate
     * @return
     */
    public void registerConsumer(DefaultGrailsMessageConsumerClass candidate) {
        // Create the adapter
        RabbitConsumerAdapter adapter = new RabbitConsumerAdapter.RabbitConsumerAdapterBuilder().build {
            delegate.rabbitContext = this
            delegate.messageConverterManager = messageConverterManager
            delegate.persistenceInterceptor = applicationContext.getBean('persistenceInterceptor')
            delegate.grailsApplication = grailsApplication
            delegate.consumer = applicationContext.getBean(candidate.fullName)
        }

        // Find the appropriate connection context
        ConnectionContext context = getConnection(adapter.getConfiguration().getConnection())

        if (!context) {
            log.warn('unable to register ${candidate.shortName} as a consumer because its connection could not be found')
            return
        }

        context.registerConsumer(adapter)
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
        return createChannel(null)
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
     * Returns the ConnectionContext associated with the default connection.
     *
     * @return
     */
    @Override
    public ConnectionContext getConnection() {
        return getConnection(null)
    }

    /**
     * Returns the ConnectionContext associated with the default connection.
     *
     * @return
     */
    @Override
    public ConnectionContext getConnection(String name) {
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

    /**
     * Loads message converters.
     */
    protected void loadMessageConverters() {
        // Register application-provided converters
        grailsApplication.messageConverterClasses.each { GrailsClass clazz ->
            messageConverterManager.registerMessageConverter(applicationContext.getBean(clazz.fullName))
        }

        // Register built-in message converters
        // Note: the order matters, we want string to be the last one
        messageConverterManager.registerMessageConverter(application.mainContext.getBean("${IntegerMessageConverter.name}"))
        messageConverterManager.registerMessageConverter(application.mainContext.getBean("${MapMessageConverter.name}"))
        messageConverterManager.registerMessageConverter(application.mainContext.getBean("${ListMessageConverter.name}"))
        messageConverterManager.registerMessageConverter(application.mainContext.getBean("${GStringMessageConverter.name}"))
        messageConverterManager.registerMessageConverter(application.mainContext.getBean("${StringMessageConverter.name}"))
    }

    /**
     * Loads message consumers.
     */
    protected void loadConsumers() {
        grailsApplication.messageConsumerClasses.each { GrailsClass clazz ->
            registerConsumer(clazz)
        }
    }

    /**
     * Loads the configuration and registers any consumers and converters.
     */
    @Override
    public void load() {
        // Load the configuration
        loadConfiguration()

        // Load message converters
        loadMessageConverters()

        // Load consumers
        loadConsumers()
    }

    /**
     * Registers a message converter with the message converter manager.
     *
     * @param converter
     */
    @Override
    @Deprecated
    public void registerMessageConverter(MessageConverter converter) {
        messageConverterManager.registerMessageConverter(converter)

    }

    /**
     * Returns a list of all registered message converters.
     *
     * @param converter
     */
    @Override
    @Deprecated
    public List<MessageConverter> getMessageConverters() {
        return messageConverterManager.getMessageConverters()
    }

    /**
     * Sets the message converter manager.
     */
    @Override
    public void setMessageConverterManager(MessageConverterManager messageConverterManager) {
        this.messageConverterManager = messageConverterManager
    }

    /**
     * Returns the message converter manager.
     */
    @Override
    public MessageConverterManager getMessageConverterManager() {
        return messageConverterManager
    }
}
