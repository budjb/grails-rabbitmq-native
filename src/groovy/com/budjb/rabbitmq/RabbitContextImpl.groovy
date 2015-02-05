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

import com.budjb.rabbitmq.connection.ConnectionConfiguration
import com.budjb.rabbitmq.connection.ConnectionContext

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
     * Grails application bean
     */
    protected GrailsApplication grailsApplication

    /**
     * Spring application context
     */
    protected ApplicationContext applicationContext

    /**
     * Logger
     */
    protected Logger log = Logger.getLogger(RabbitContextImpl)

    /**
     * List of current connection contexts.
     */
    protected List<ConnectionContext> connections = []

    /**
     * The message converter manager.
     */
    protected MessageConverterManager messageConverterManager

    /**
     * Loads and initializes the configuration.
     */
    protected void loadConfiguration() {
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
            throw new Exception('RabbitMQ connection configuration is not a Map or a Closure')
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
        connections*.openConnection()

        // Set up any configured queues/exchanges
        configureQueues()

        // Start consumers if requested
        if (!skipConsumers) {
            startConsumers()
        }
    }

    /**
     * Creates the exchanges and queues that are defined in the Grails configuration.
     *
     * TODO move this into its own bean
     */
    protected void configureQueues() {
        // Skip if the config isn't defined
        if (!(grailsApplication.config.rabbitmq?.queues instanceof Closure)) {
            return
        }

        // Grab the config closure
        Closure config = grailsApplication.config.rabbitmq.queues

        // Create the queue builder
        QueueBuilder queueBuilder = new QueueBuilder(this)

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
            delegate.consumer = applicationContext.getBean(candidate.fullName)
            delegate.grailsApplication = grailsApplication
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
            context = connections.find { it.getConfiguration().getIsDefault() == true }

            if (!context) {
                log.error("no default connection found")
                return null
            }
        }
        else {
            context = connections.find { it.getConfiguration().getName() == name }

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
        grailsApplication.getArtefacts('MessageConverter').each {
            Object consumer = applicationContext.getBean(it.fullName)
            messageConverterManager.registerMessageConverter(consumer)
        }

        // Register built-in message converters
        // Note: the order matters, we want string to be the last one
        messageConverterManager.registerMessageConverter(new IntegerMessageConverter())
        messageConverterManager.registerMessageConverter(new MapMessageConverter())
        messageConverterManager.registerMessageConverter(new ListMessageConverter())
        messageConverterManager.registerMessageConverter(new GStringMessageConverter())
        messageConverterManager.registerMessageConverter(new StringMessageConverter())
    }

    /**
     * Loads message consumers.
     */
    protected void loadConsumers() {
        grailsApplication.getArtefacts('MessageConverter').each { registerConsumer(it) }
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
     * Returns the grails application bean.
     */
    @Override
    public GrailsApplication getGrailsApplication() {
        return grailsApplication
    }

    /**
     * Sets the grails application bean.
     */
    @Override
    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
    }

    /**
     * Returns the application context bean.
     */
    @Override
    public ApplicationContext getApplicationContext() {
        return applicationContext
    }

    /**
     * Sets the application context bean.
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext
    }
}
