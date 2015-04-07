/*
 * Copyright 2015 Bud Byrd
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
package com.budjb.rabbitmq.consumer

import com.budjb.rabbitmq.connection.ConnectionContext
import com.budjb.rabbitmq.connection.ConnectionManager
import com.budjb.rabbitmq.converter.MessageConverterManager
import com.budjb.rabbitmq.exception.ContextNotFoundException
import com.budjb.rabbitmq.exception.MissingConfigurationException
import com.budjb.rabbitmq.publisher.RabbitMessagePublisher
import grails.core.GrailsApplication
import grails.core.GrailsClass
import grails.persistence.support.PersistenceContextInterceptor
import org.apache.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

class ConsumerManagerImpl implements ConsumerManager, ApplicationContextAware {
    /**
     * Name of the configuration variable a consumer is expected to define.
     */
    static final String RABBIT_CONFIG_NAME = 'rabbitConfig'

    /**
     * Grails application bean.
     */
    @Autowired
    GrailsApplication grailsApplication

    /**
     * Hibernate object used to bind a session to the current thread.
     *
     * This will be null if Hibernate is not present.
     */
    @Autowired(required = false)
    PersistenceContextInterceptor persistenceInterceptor

    /**
     * Message converter manager.
     */
    MessageConverterManager messageConverterManager

    /**
     * Rabbit message publisher.
     */
    RabbitMessagePublisher rabbitMessagePublisher

    /**
     * Connection manager.
     */
    ConnectionManager connectionManager

    /**
     * Application context.
     */
    ApplicationContext applicationContext

    /**
     * Logger.
     */
    private Logger log = Logger.getLogger(ConsumerManagerImpl)

    /**
     * Registered consumers.
     */
    protected List<ConsumerContext> consumers = []

    /**
     * Loads any message consumer artefacts.
     */
    @Override
    void load() {
        grailsApplication.getArtefacts('MessageConsumer').each {
            try {
                register(createContext(it))
            }
            catch (MissingConfigurationException e) {
                log.warn("not loading consumer '${it.shortName}' because its configuration is missing")
            }
        }
    }

    /**
     * Starts all registered consumers.
     */
    @Override
    void start() {
        consumers.each {
            try {
                start(it)
            }
            catch (IllegalStateException e) {
                // Continue...
            }
        }
    }

    /**
     * Starts a specific consumer.
     *
     * @param context
     */
    @Override
    void start(ConsumerContext context) {
        context.start()
    }

    /**
     * Starts a specific consumer based on its name.
     *
     * @param name
     * @throws ContextNotFoundException
     */
    @Override
    void start(String name) throws ContextNotFoundException {
        start(getContext(name))
    }

    /**
     * Stops all consumers.
     */
    @Override
    void stop() {
        consumers.each { stop(it) }
    }

    /**
     * Stops a specific consumer.
     *
     * @param context
     */
    @Override
    void stop(ConsumerContext context) {
        context.stop()
    }

    /**
     * Stops a specific consumer based on its name.
     *
     * @param name
     * @throws ContextNotFoundException
     */
    @Override
    void stop(String name) throws ContextNotFoundException {
        stop(getContext(name))
    }

    /**
     * Stops and removes all consumers.
     */
    @Override
    void reset() {
        consumers.each { unregister(it) }
    }

    /**
     * Registers a consumer.
     *
     * @param context
     */
    @Override
    void register(ConsumerContext context) {
        try {
            unregister(getContext(context.id))
        }
        catch (ContextNotFoundException e) {
            // Continue...
        }

        consumers << context
    }

    /**
     * Stops and un-registers a consumer.
     *
     * @param context
     */
    @Override
    void unregister(ConsumerContext context) {
        stop(context)
        consumers -= context
    }

    /**
     * Returns the consumer adapter whose consumer has the given name.
     *
     * @param name
     * @return
     * @throws ContextNotFoundException
     */
    @Override
    ConsumerContext getContext(String name) throws ContextNotFoundException {
        ConsumerContext adapter = consumers.find { it.id == name }

        if (!adapter) {
            throw new ContextNotFoundException("consumer '${name}' is not registered")
        }

        return adapter
    }

    /**
     * Create a consumer context with the given consumer object instance.
     *
     * @param consumer
     * @return
     */
    @Override
    ConsumerContext createContext(MessageConsumer consumer) throws MissingConfigurationException {
        return new ConsumerContextImpl(
            loadConsumerConfiguration(consumer),
            consumer,
            connectionManager,
            messageConverterManager,
            persistenceInterceptor,
            rabbitMessagePublisher
        )
    }

    /**
     * Create a consumer context withe consumer represented by the given Grails artefact.
     *
     * @param artefact
     * @return
     */
    @Override
    ConsumerContext createContext(GrailsClass artefact) {
        return createContext(applicationContext.getBean(artefact.propertyName) as MessageConsumer)
    }

    /**
     * Attempts to load a consumer's configuration.
     *
     * @param consumer
     * @return A ConsumerConfiguration instance.
     * @throws MissingConfigurationException
     */
    protected ConsumerConfiguration loadConsumerConfiguration(MessageConsumer consumer) throws MissingConfigurationException {
        Map configuration = consumer.getRabbitConfig()

        if (!configuration) {
            configuration = loadConsumerApplicationConfiguration(consumer)
        }

        if (!configuration) {
            throw new MissingConfigurationException("consumer has no configuration defined either within either its class or the application configuration")
        }

        return new ConsumerConfigurationImpl(configuration)
    }

    /**
     * Finds and returns a consumer's central configuration, or null if it isn't defined.
     *
     * @return
     */
    protected Map loadConsumerApplicationConfiguration(MessageConsumer consumer) {
        def configuration = grailsApplication.config.rabbitmq.consumers."${consumer.getClass().simpleName}"

        if (!configuration || !Map.class.isAssignableFrom(configuration.getClass())) {
            return null
        }

        return configuration
    }

    /**
     * Starts all consumers associated with the given connection context.
     *
     * @param connectionContext
     */
    @Override
    void start(ConnectionContext connectionContext) {
        getContexts(connectionContext).each {
            try {
                it.start()
            }
            catch (IllegalStateException e) {
                // Continue...
            }
        }
    }

    /**
     * Stops all consumers associated with the given connection context.
     *
     * @param connectionContext
     */
    @Override
    void stop(ConnectionContext connectionContext) {
        getContexts(connectionContext).each { stop(it) }
    }

    /**
     * Retrieves all consumer contexts associated with the given connection context.
     *
     * @param connectionContext
     * @return
     */
    @Override
    List<ConsumerContext> getContexts(ConnectionContext connectionContext) {
        return consumers.findAll { it.connectionName == connectionContext.id }
    }

    /**
     * Returns a list of all registered contexts.
     *
     * @return
     */
    @Override
    List<ConsumerContext> getContexts() {
        return consumers
    }
}
