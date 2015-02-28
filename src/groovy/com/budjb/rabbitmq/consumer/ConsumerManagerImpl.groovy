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
package com.budjb.rabbitmq.consumer

import com.budjb.rabbitmq.connection.ConnectionManager
import com.budjb.rabbitmq.converter.MessageConverterManager
import com.budjb.rabbitmq.exception.ContextNotFoundException
import com.budjb.rabbitmq.publisher.RabbitMessagePublisher
import org.apache.log4j.Logger
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClass
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

class ConsumerManagerImpl implements ConsumerManager, ApplicationContextAware {
    /**
     * Grails application bean.
     */
    protected GrailsApplication grailsApplication

    /**
     * Hibernate object used to bind a session to the current thread.
     *
     * This will be null if Hibernate is not present.
     */
    protected Object persistenceInterceptor

    /**
     * Message converter manager.
     */
    protected MessageConverterManager messageConverterManager

    /**
     * Rabbit message publisher.
     */
    protected RabbitMessagePublisher rabbitMessagePublisher

    /**
     * Connection manager.
     */
    protected ConnectionManager connectionManager

    /**
     * Application context.
     */
    protected ApplicationContext applicationContext

    /**
     * Logger.
     */
    protected Logger log = Logger.getLogger(ConsumerManagerImpl)

    /**
     * Registered consumers.
     */
    protected List<ConsumerContextImpl> consumers

    /**
     * Creates a new ConsumerAdapter.
     */
    public ConsumerContextImpl createConsumerAdapter(Object consumer) {
        return new ConsumerContextImpl(
            consumer,
            grailsApplication,
            connectionManager,
            messageConverterManager,
            persistenceInterceptor,
            rabbitMessagePublisher
        )
    }

    /**
     * Loads any message consumer artefacts.
     */
    @Override
    public void load() {
        grailsApplication.getArtefacts('MessageConsumer').each { register(it) }
    }

    /**
     * Starts all registered consumers.
     */
    @Override
    void start() {
        consumers.each { start(it) }
    }

    /**
     * Starts a specific consumer.
     *
     * @param context
     */
    @Override
    void start(ConsumerContextImpl context) {
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
    void stop(ConsumerContextImpl context) {
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
     * Registers a consumer based on its Grails artefact.
     *
     * @param artefact
     */
    @Override
    void register(GrailsClass artefact) {
        register(applicationContext.getBean(artefact.propertyName))
    }

    /**
     * Registers a new consumer with the provided consumer instance.
     *
     * @param consumer
     */
    @Override
    void register(Object consumer) {
        register(createConsumerAdapter(consumer))
    }

    /**
     * Registers a consumer.
     *
     * @param context
     */
    @Override
    void register(ConsumerContextImpl context) {
        if (!context.isValid()) {
            log.warn("not registering consumer '${context.id}' because it is not valid")
            return
        }

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
    void unregister(ConsumerContextImpl context) {
        stop(context)
        consumers.remove(context)
    }

    /**
     * Un-registers the given consumer instance.
     *
     * @param consumer
     * @throws ContextNotFoundException
     */
    @Override
    void unregister(Object consumer) throws ContextNotFoundException {
        ConsumerContextImpl adapter = consumers.find { it.consumer == consumer }

        if (!adapter) {
            throw new ContextNotFoundException("provided consumer is not registered")
        }

        unregister(adapter)
    }

    /**
     * Returns the consumer adapter whose consumer has the given name.
     *
     * @param name
     * @return
     * @throws ContextNotFoundException
     */
    @Override
    ConsumerContextImpl getContext(String name) throws ContextNotFoundException {
        ConsumerContextImpl adapter = consumers.find { it.id == name }

        if (!adapter) {
            throw new ContextNotFoundException("consumer '${name}' is not registered")
        }

        return adapter
    }

    /**
     * Sets the application context.
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext
    }

    /**
     * Sets the grails application bean.
     */
    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
    }

    /**
     * Sets the connection manager.
     */
    public void setConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager
    }

    /**
     * Sets the message converter manager.
     */
    public void setMessageConverterManager(MessageConverterManager messageConverterManager) {
        this.messageConverterManager = messageConverterManager
    }

    /**
     * Sets the persistence interceptor, if available.
     */
    public void setPersistenceInterceptor(def persistenceInterceptor) {
        this.persistenceInterceptor = persistenceInterceptor
    }

    /**
     * Sets the rabbit message builder.
     */
    public void setRabbitMessagePublisher(RabbitMessagePublisher rabbitMessagePublisher) {
        this.rabbitMessagePublisher = rabbitMessagePublisher
    }

    /**
     * Creates a new managed context.
     *
     * @param configuration
     * @return
     */
    @Override
    ConsumerContext createContext(ConsumerConfiguration configuration) {
        return null
    }

    /**
     * Starts a specified context.
     *
     * @param context
     */
    @Override
    void start(ConsumerContext context) {

    }

    /**
     * Stops a specified context.
     *
     * @param context
     */
    @Override
    void stop(ConsumerContext context) {

    }

    /**
     * Registers a new managed context.
     *
     * @param context
     */
    @Override
    void register(ConsumerContext context) {

    }

    /**
     * Un-registers a managed context.
     *
     * @param context
     */
    @Override
    void unregister(ConsumerContext context) {

    }
}
