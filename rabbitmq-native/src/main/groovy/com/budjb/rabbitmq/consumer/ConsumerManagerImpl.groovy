/*
 * Copyright 2013-2017 Bud Byrd
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

import com.budjb.rabbitmq.RunningState
import com.budjb.rabbitmq.connection.ConnectionContext
import com.budjb.rabbitmq.connection.ConnectionManager
import com.budjb.rabbitmq.converter.MessageConverterManager
import com.budjb.rabbitmq.event.ConsumerManagerStartedEvent
import com.budjb.rabbitmq.event.ConsumerManagerStartingEvent
import com.budjb.rabbitmq.event.ConsumerManagerStoppedEvent
import com.budjb.rabbitmq.event.ConsumerManagerStoppingEvent
import com.budjb.rabbitmq.exception.ContextNotFoundException
import com.budjb.rabbitmq.exception.MissingConfigurationException
import com.budjb.rabbitmq.publisher.RabbitMessagePublisher
import grails.core.GrailsApplication
import grails.core.GrailsClass
import grails.persistence.support.PersistenceContextInterceptor
import groovyx.gpars.GParsPool
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationEventPublisher

class ConsumerManagerImpl implements ConsumerManager, ApplicationContextAware {
    /**
     * Grails application bean.
     */
    @Autowired
    GrailsApplication grailsApplication

    /**
     * Spring application event publisher.
     */
    @Autowired
    ApplicationEventPublisher applicationEventPublisher

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
    @Autowired
    MessageConverterManager messageConverterManager

    /**
     * Rabbit message publisher.
     */
    @Autowired
    RabbitMessagePublisher rabbitMessagePublisher

    /**
     * Connection manager.
     */
    @Autowired
    ConnectionManager connectionManager

    /**
     * Application context.
     */
    ApplicationContext applicationContext

    /**
     * Logger.
     */
    Logger log = LoggerFactory.getLogger(ConsumerManagerImpl)

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
            catch (MissingConfigurationException ignored) {
                log.warn("not loading consumer '${it.shortName}' because its configuration is missing")
            }
        }
    }

    /**
     * Starts all registered consumers.
     */
    @Override
    void start() {
        RunningState runningState = getRunningState()

        if (runningState == RunningState.SHUTTING_DOWN) {
            throw new IllegalStateException('can not start consumers when they are in the process of shutting down')
        }

        applicationEventPublisher.publishEvent(new ConsumerManagerStartingEvent(this))

        consumers.each {
            start(it)
        }

        applicationEventPublisher.publishEvent(new ConsumerManagerStartedEvent(this))
    }

    /**
     * Starts a specific consumer.
     *
     * @param context
     */
    @Override
    void start(ConsumerContext context) {
        if (context.getRunningState() == RunningState.STOPPED) {
            context.start()
        }
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
        applicationEventPublisher.publishEvent(new ConsumerManagerStoppingEvent(this))

        consumers.each {
            stop(it)
        }

        applicationEventPublisher.publishEvent(new ConsumerManagerStoppedEvent(this))
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
            log.trace("context with id ${context.id} not found; this is ok", e)
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
        ConsumerContext adapter = consumers.find { it.getId() == name } ?: consumers.find { it.getName() == name }

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
    ConsumerContext createContext(Object consumer) {
        if (!MessageConsumer.isInstance(consumer)) {
            consumer = new GrailsMessageConsumerWrapper(consumer, grailsApplication, messageConverterManager)
        }

        return new ConsumerContextImpl(
            (MessageConsumer) consumer,
            connectionManager,
            persistenceInterceptor,
            rabbitMessagePublisher,
            applicationEventPublisher
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
        return createContext(applicationContext.getBean(artefact.fullName))
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
                log.trace("problem starting consumer ${it.id}; this is ok", e)
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
        return consumers.findAll {
            return (connectionContext.isDefault && !it.connectionName) || (it.connectionName == connectionContext.id)
        }
    }

    /**
     * Performs a graceful shutdown of all consumers.
     */
    @Override
    void shutdown() {
        GParsPool.withPool {
            consumers.eachParallel {
                it.shutdown()
            }
        }
    }

    /**
     * Performs a graceful shutdown of the given consumer context
     *
     * @param consumerContext
     */
    @Override
    void shutdown(ConsumerContext consumerContext) {
        consumerContext.shutdown()
    }

    /**
     * Performs a graceful shutdown of the consumer with the given name.
     *
     * @param name
     */
    @Override
    void shutdown(String name) {
        getContext(name).shutdown()
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

    /**
     * Returns the state of the contexts the manager.
     *
     * @return
     */
    @Override
    RunningState getRunningState() {
        List<RunningState> runningStates = consumers*.getRunningState()

        if (runningStates.any { it == RunningState.SHUTTING_DOWN }) {
            return RunningState.SHUTTING_DOWN
        }
        else if (runningStates.every { it == RunningState.STOPPED }) {
            return RunningState.STOPPED
        }
        else {
            return RunningState.RUNNING
        }
    }
}
