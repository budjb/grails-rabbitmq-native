/*
 * Copyright 2016 Bud Byrd
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
import com.budjb.rabbitmq.exception.ContextNotFoundException
import com.budjb.rabbitmq.exception.MessageConvertException
import com.budjb.rabbitmq.publisher.RabbitMessageProperties
import com.budjb.rabbitmq.publisher.RabbitMessagePublisher
import com.budjb.rabbitmq.report.ConsumerReport
import com.rabbitmq.client.Channel
import grails.persistence.support.PersistenceContextInterceptor
import groovyx.gpars.GParsPool
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.util.ClassUtils

import java.lang.reflect.Method
import java.lang.reflect.Modifier

class ConsumerContextImpl implements ConsumerContext {
    /**
     * Name of the handler method a consumer is expected to define.
     */
    private static final String RABBIT_HANDLER_NAME = 'handleMessage'

    /**
     * Name of the method that will be called when a message is received, but before it is processed.
     */
    static final String CONSUME_ON_RECEIVE_METHOD_NAME = 'onReceive'

    /**
     * Name of the method that will be called when a message has been successfully processed.
     */
    static final String CONSUME_ON_SUCCESS_METHOD_NAME = 'onSuccess'

    /**
     * Name of the method that will be called after a message has attempted to be processed, whether it worked or not.
     */
    static final String CONSUME_ON_COMPLETE_METHOD_NAME = 'onComplete'

    /**
     * Name of the method that will be called when an exception is caught handling the message.
     */
    static final String CONSUME_ON_FAILURE_METHOD_NAME = 'onFailure'

    /**
     * Logger.
     */
    private Logger log = LoggerFactory.getLogger(ConsumerContextImpl)

    /**
     * Consumer bean.
     */
    Object consumer

    /**
     * The consumer's configuration.
     */
    ConsumerConfiguration configuration

    /**
     * Connection manager.
     */
    ConnectionManager connectionManager

    /**
     * Persistence interceptor for Hibernate session handling.
     */
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
     * List of active rabbit consumers.
     */
    protected List<RabbitMessageHandler> consumers = []

    /**
     * List of message handler method descriptors.
     */
    protected List<Class<?>> validHandlerTargets = []

    /**
     * Constructor.
     *
     * @param clazz
     * @param grailsApplication
     */
    ConsumerContextImpl(
        ConsumerConfiguration configuration,
        Object consumer,
        ConnectionManager connectionManager,
        MessageConverterManager messageConverterManager,
        PersistenceContextInterceptor persistenceInterceptor,
        RabbitMessagePublisher rabbitMessagePublisher) {

        this.consumer = consumer
        this.configuration = configuration
        this.connectionManager = connectionManager
        this.messageConverterManager = messageConverterManager
        this.persistenceInterceptor = persistenceInterceptor
        this.rabbitMessagePublisher = rabbitMessagePublisher

        loadValidHandlerTargets()
    }

    /**
     * Return the name of the connection the consumer belongs to.
     *
     * @return
     */
    @Override
    String getConnectionName() {
        return getConfiguration().getConnection()
    }

    /**
     * Determines if a handler is a RabbitMQ consumer.
     *
     * @return
     */
    @Override
    boolean isValid() {
        ConsumerConfiguration configuration
        try {
            configuration = getConfiguration()
        }
        catch (Exception ignore) {
            log.error("unable to retrieve configuration for consumer '${getId()}")
            return false
        }

        if (!configuration) {
            return false
        }

        if (!configuration.isValid()) {
            return false
        }

        if (!getAllMethods(consumer.getClass()).any { isValidHandlerMethod(it) }) {
            return false
        }

        return true
    }

    /**
     * Returns the consumer's short name.
     *
     * @return
     */
    @Override
    String getId() {
        return consumer.getClass().getSimpleName()
    }

    /**
     * Starts a consumer.
     *
     * @throws IllegalStateException
     */
    @Override
    void start() throws IllegalStateException {
        if (getRunningState() != RunningState.STOPPED) {
            throw new IllegalStateException("attempted to start consumer '${getId()}' but it is already started")
        }

        if (!isValid()) {
            log.warn("not starting consumer '${getId()}' because it is not valid")
            return
        }

        ConsumerConfiguration configuration = getConfiguration()

        String connectionName = configuration.getConnection()

        ConnectionContext connectionContext
        try {
            connectionContext = connectionManager.getContext(connectionName)
        }
        catch (ContextNotFoundException ignore) {
            log.warn("not starting consumer '${getId()}' because a suitable connection could not be found")
            return
        }

        if (connectionContext.getRunningState() != RunningState.RUNNING) {
            throw new IllegalStateException("attempted to start consumer '${getId()}' but its connection is not started")
        }

        if (configuration.queue) {
            log.debug("starting consumer '${getId()}' on connection '${connectionContext.id}' with ${configuration.consumers} consumer(s)")

            configuration.consumers.times {
                Channel channel = connectionContext.createChannel()

                String queue = configuration.getQueue()

                channel.basicQos(configuration.getPrefetchCount())

                RabbitMessageHandler consumer = new RabbitMessageHandler(channel, queue, this, connectionContext)

                channel.basicConsume(
                    queue,
                    configuration.getAutoAck() == AutoAck.ALWAYS,
                    consumer
                )

                consumers << consumer
            }
        }
        else {
            log.debug("starting consumer '${getId()}' on connection '${connectionContext.id}'")

            Channel channel = connectionContext.createChannel()

            String queue = channel.queueDeclare().getQueue()
            if (!configuration.getBinding() || configuration.getBinding() instanceof String) {
                channel.queueBind(queue, configuration.getExchange(), (String) configuration.getBinding() ?: '')
            }
            else if (configuration.getBinding() instanceof Map) {
                channel.queueBind(queue, configuration.getExchange(), '', (configuration.getBinding() as Map) + ['x-match': configuration.getMatch()])
            }

            channel.basicQos(configuration.getPrefetchCount())

            RabbitMessageHandler consumer = new RabbitMessageHandler(channel, queue, this, connectionContext)

            channel.basicConsume(
                queue,
                configuration.getAutoAck() == AutoAck.ALWAYS,
                consumer
            )

            consumers << consumer
        }
    }

    /**
     * Closes all channels and clears all consumers.
     */
    @Override
    void stop() {
        if (getRunningState() == RunningState.STOPPED) {
            return
        }

        consumers.each {
            it.stop()
        }

        consumers.clear()

        log.debug("stopped consumer '${getId()}' on connection '${getConnectionName()}'")
    }

    /**
     * Performs a graceful shutdown.
     */
    @Override
    void shutdown() {
        if (getRunningState() != RunningState.RUNNING) {
            return
        }

        log.debug("shutting down consumer '${getId()}' on connection '${getConnectionName()}'")

        GParsPool.withPool {
            consumers.eachParallel { it.shutdown() }
        }

        consumers.clear()

        log.debug("stopped consumer '${getId()}' on connection '${getConnectionName()}'")
    }

    /**
     * Generate a status report about the context and its consumers.
     *
     * @return
     */
    @Override
    ConsumerReport getStatusReport() {
        ConsumerReport report = new ConsumerReport()

        ConsumerConfiguration configuration = getConfiguration()

        report.name = getId()
        report.fullName = consumer.getClass().getName()

        report.runningState = getRunningState()

        report.queue = configuration.queue ?: consumers.size() > 0 ? consumers[0].queue : null

        report.numConfigured = configuration.consumers
        report.numConsuming = consumers.count { it.getRunningState() == RunningState.RUNNING }
        report.numProcessing = consumers.count { it.isProcessing() }
        report.load = report.numConsuming > 0 ? report.numProcessing / report.numConsuming * 100 : 0

        return report
    }

    /**
     * Processes and delivers an incoming message to the consumer.
     *
     * @param context
     */
    @Override
    void deliverMessage(MessageContext context) {
        Object convertedBody
        try {
            convertedBody = convertMessage(context)
        }
        catch (Throwable e) {
            log.error("unexpected exception ${e.getClass()} encountered converting incoming request with handler ${getId()}", e)
            return
        }

        Object response
        try {
            response = invokeHandler(convertedBody, context)
        }
        catch (Throwable e) {
            log.error("unexpected exception ${e.getClass()} encountered in the rabbit consumer associated with handler ${getId()}", e)
            return
        }

        if (context.properties.replyTo && response != null) {
            try {
                if (context.properties.replyTo && response) {
                    rabbitMessagePublisher.send(new RabbitMessageProperties(
                        channel: context.channel,
                        routingKey: context.properties.replyTo,
                        correlationId: context.properties.correlationId,
                        body: response
                    ))
                }
            }
            catch (Throwable e) {
                log.error("unexpected exception ${e.getClass()} encountered while responding from an RPC call with handler ${getId()}", e)
            }
        }
    }

    /**
     * Hands off the message to the handler (if a valid one is found).
     *
     * @param messageContext
     * @return Any returned value from the handler.
     */
    protected Object invokeHandler(Object body, MessageContext messageContext) {
        Method handler = findHandlerForClass(body.getClass())

        if (!handler) {
            throw new IllegalStateException("Could not find the handler method for class type ${body.getClass()}. This shoult not occur. Please file a bug.")
        }

        openSession()

        onReceive(messageContext)

        try {
            if (configuration.getTransacted()) {
                messageContext.getChannel().txSelect()
            }

            Object response
            if (handler.getParameterCount() == 1) {
                if (ClassUtils.isAssignable(handler.getParameterTypes()[0], MessageContext)) {
                    response = handler.invoke(consumer, messageContext)
                }
                else {
                    response = handler.invoke(consumer, body)
                }
            }
            else {
                response = handler.invoke(consumer, body, messageContext)
            }

            if (configuration.getAutoAck() == AutoAck.POST) {
                messageContext.getChannel().basicAck(messageContext.getEnvelope().deliveryTag, false)
            }

            if (configuration.getTransacted()) {
                messageContext.getChannel().txCommit()
            }

            onSuccess(messageContext)

            return response
        }
        catch (Exception e) {
            if (configuration.getTransacted()) {
                messageContext.getChannel().txRollback()
            }

            if (configuration.getAutoAck() == AutoAck.POST) {
                messageContext.getChannel().basicReject(messageContext.getEnvelope().deliveryTag, configuration.getRetry())
            }

            if (configuration.getTransacted()) {
                log.error("transaction rolled back due to unhandled exception ${e.getClass().name} caught in RabbitMQ message handler for consumer ${getId()}", e)
            }
            else {
                log.error("unhandled exception ${e.getClass().name} caught in RabbitMQ message handler for consumer ${getId()}", e)
            }

            onFailure(messageContext)

            return null
        }
        finally {
            onComplete(messageContext)

            closeSession()
        }
    }

    /**
     * Attempts to convert the body of the incoming message from a byte array.
     * The output of this method is dependent on the consumer's configuration,
     * the content-type of the message, and the existence of an appropriately
     * defined handler for the converted type.
     *
     * @param context
     * @return
     */
    protected Object convertMessage(MessageContext context) {
        ConsumerConfiguration configuration = getConfiguration()

        if (configuration.getConvert() == MessageConvertMethod.DISABLED) {
            return context.getBody()
        }

        if (context.getProperties().getContentType()) {
            try {
                return messageConverterManager.convertFromBytes(context.getBody(), validHandlerTargets, context.getProperties().getContentType())
            }
            catch (MessageConvertException ignore) {
                // Continue
            }
        }

        if (configuration.getConvert() == MessageConvertMethod.HEADER) {
            return context.getBody()
        }

        try {
            return messageConverterManager.convertFromBytes(context.getBody(), validHandlerTargets)
        }
        catch (MessageConvertException ignore) {
            // Continue
        }

        return context.getBody()
    }

    /**
     * Initiates one of the callback methods if the method exists.
     *
     * @param methodName
     * @param context
     */
    protected void doCallback(String methodName, MessageContext context) {
        if (!consumer.class.metaClass.methods.find { it.name == methodName }) {
            return
        }

        consumer."${methodName}"(context)
    }

    /**
     * Initiates the "on received" callback.
     *
     * @param context
     */
    protected void onReceive(MessageContext context) {
        doCallback(CONSUME_ON_RECEIVE_METHOD_NAME, context)
    }

    /**
     * Initiates the "on success" callback.
     *
     * @param context
     */
    protected void onSuccess(MessageContext context) {
        doCallback(CONSUME_ON_SUCCESS_METHOD_NAME, context)
    }

    /**
     * Initiates the "on failure" callback.
     *
     * @param context
     */
    protected void onComplete(MessageContext context) {
        doCallback(CONSUME_ON_COMPLETE_METHOD_NAME, context)
    }

    /**
     * Initiates the "on complete" callback.
     *
     * @param context
     */
    protected void onFailure(MessageContext context) {
        doCallback(CONSUME_ON_FAILURE_METHOD_NAME, context)
    }

    /**
     * Binds a Hibernate session to the current thread if Hibernate is present.
     */
    protected void openSession() {
        if (!persistenceInterceptor) {
            return
        }

        persistenceInterceptor.init()
    }

    /**
     * Closes the bound Hibernate session if Hibernate is present.
     */
    protected void closeSession() {
        if (!persistenceInterceptor) {
            return
        }

        persistenceInterceptor.flush()
        persistenceInterceptor.destroy()
    }

    /**
     * Returns whether the consumer is active.
     *
     * @return
     */
    RunningState getRunningState() {
        if (consumers.size() == 0) {
            return RunningState.STOPPED
        }

        List<RunningState> states = consumers*.getRunningState()

        if (states.any { it == RunningState.SHUTTING_DOWN }) {
            return RunningState.SHUTTING_DOWN
        }

        if (states.every { it == RunningState.STOPPED }) {
            return RunningState.STOPPED
        }

        return RunningState.RUNNING
    }

    /**
     * Returns all methods, both declared and inherited, from the given class.
     *
     * @param clazz
     * @return
     */
    protected List<Method> getAllMethods(Class<?> clazz) {
        if (clazz == Object.class) {
            return []
        }

        return clazz.getDeclaredMethods() + getAllMethods(clazz.getSuperclass())
    }

    /**
     * Inspect all methods of the given object and return a descriptor for any
     * method that is a candidate as a message handler.
     *
     * @param consumer
     * @return
     */
    protected void loadValidHandlerTargets() {
        List<Method> matching = getAllMethods(consumer.getClass()).findAll {
            return isValidHandlerMethod(it)
        }

        validHandlerTargets = matching.collect { it.getParameterTypes()[0] }
    }

    /**
     * Determines whether the given method meets the criteria for serving as a consumer handler.
     *
     * @param method
     * @return
     */
    protected boolean isValidHandlerMethod(Method method) {
        if (method.getName() != RABBIT_HANDLER_NAME) {
            return false
        }

        if (!Modifier.isPublic(method.getModifiers())) {
            return false
        }

        if (!(method.getParameterTypes().size() in [1, 2])) {
            return false
        }

        return true
    }

    /**
     * Find a handler method for the given object type.
     *
     * Preference will be first given to strongly typed handlers, and Object (def) will
     * be tried last.
     *
     * If no handler taking a body is found, a last-ditch effort to find a {@link MessageContext}
     * handler will be attempted.
     *
     * @param clazz Object type to deliver to the consumer.
     * @return An appropriate handler method for the given type, or null if none are found.
     */
    protected Method findHandlerForClass(Class<?> clazz) {
        List<Method> handlers = getAllMethods(consumer.getClass()).findAll { isValidHandlerMethod(it) }

        Method method = handlers.find {
            int paramCount = it.getParameterCount()

            if (paramCount < 1 || paramCount > 2) {
                return false
            }

            if (it.getParameterTypes()[0] == Object.class) {
                return false
            }

            if (!ClassUtils.isAssignable(it.getParameterTypes()[0], clazz)) {
                return false
            }

            if (it.getParameterCount() == 2) {
                if (!ClassUtils.isAssignable(it.getParameterTypes()[1], MessageContext)) {
                    return false
                }
            }

            return true
        }

        if (!method) {
            method = handlers.find {
                int paramCount = it.getParameterCount()

                if (paramCount < 1 || paramCount > 2) {
                    return false
                }

                if (it.getParameterTypes()[0] != Object.class) {
                    return false
                }

                if (it.getParameterCount() == 2) {
                    if (!ClassUtils.isAssignable(it.getParameterTypes()[1], MessageContext)) {
                        return false
                    }
                }

                return true
            }
        }

        if (!method) {
            method = handlers.find {
                it.getParameterCount() == 1 && ClassUtils.isAssignable(it.getParameterTypes()[0], MessageContext)
            }
        }

        return method
    }
}
