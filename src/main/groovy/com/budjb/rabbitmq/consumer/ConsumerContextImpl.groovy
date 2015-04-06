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
import com.budjb.rabbitmq.exception.MessageConvertException
import com.budjb.rabbitmq.publisher.RabbitMessagePublisher
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import grails.persistence.support.PersistenceContextInterceptor
import org.apache.log4j.Logger

import java.lang.reflect.Method

class ConsumerContextImpl implements ConsumerContext {
    /**
     * Consumer object used to receive messages from the RabbitMQ library.
     */
    private class RabbitConsumer extends DefaultConsumer {
        /**
         * Consumer context containing the context for this consumer.
         */
        ConsumerContextImpl context

        /**
         * Connection context associated with the consumer.
         */
        ConnectionContext connectionContext

        /**
         * Consumer tag.
         */
        String consumerTag

        /**
         * Constructs an instance of a consumer.
         *
         * @param channel
         * @param context
         */
        private RabbitConsumer(Channel channel, ConsumerContextImpl context, ConnectionContext connectionContext) {
            // Run the parent
            super(channel)

            // Store the context
            this.context = context

            // Store the connection context
            this.connectionContext = connectionContext
        }

        /**
         * Passes delivery of a message back to the context for processing.
         *
         * @param consumerTag
         * @param envelope
         * @param properties
         * @param body
         */
        @Override
        void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
            // Wrap up the parameters into a context
            MessageContext context = new MessageContext(
                channel: channel,
                consumerTag: consumerTag,
                envelope: envelope,
                properties: properties,
                body: body,
                connectionContext: connectionContext
            )

            // Hand off the message to the context.
            ConsumerContextImpl.this.deliverMessage(context)
        }
    }

    /**
     * Name of the handler method a consumer is expected to define.
     */
    static final String RABBIT_HANDLER_NAME = 'handleMessage'

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
    private Logger log = Logger.getLogger(ConsumerContextImpl)

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
    protected List<RabbitConsumer> consumers = []

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
        // Get the configuration
        ConsumerConfiguration configuration
        try {
            configuration = getConfiguration()
        }
        catch (Exception e) {
            log.error("unable to retrieve configuration for consumer '${getId()}")
            return false
        }

        // Check if there is either a local or central configuration
        if (!configuration) {
            return false
        }

        // Check if the configuration is invalid
        if (!configuration.isValid()) {
            return false
        }

        // Check if we find any handler defined
        if (!consumer.getClass().getDeclaredMethods().any { it.name == RABBIT_HANDLER_NAME }) {
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
        if (consumers.size()) {
            throw new IllegalStateException("attempted to start consumer '${getId()}' but it is already started")
        }

        // Ensure the configuration is valid
        if (!isValid()) {
            log.warn("not starting consumer '${getId()}' because it is not valid")
            return
        }

        // Get the configuration
        ConsumerConfiguration configuration = getConfiguration()

        // Get the connection name
        String connectionName = configuration.connection

        // Get the proper connection
        ConnectionContext connectionContext
        try {
            connectionContext = connectionManager.getContext(connectionName)
        }
        catch (ContextNotFoundException e) {
            log.warn("not starting consumer '${getId()}' because a suitable connection could not be found")
            return
        }

        // Start the consumers
        if (configuration.queue) {
            // Log our intentions
            log.debug("starting consumer '${getId()}' on connection '${connectionContext.id}' with ${configuration.consumers} consumer(s)")

            // Create all requested consumer instances
            configuration.consumers.times {
                // Create the channel
                Channel channel = connectionContext.createChannel()

                // Determine the queue
                String queue = configuration.queue

                // Set the QOS
                channel.basicQos(configuration.prefetchCount)

                // Create the rabbit consumer object
                RabbitConsumer consumer = new RabbitConsumer(channel, this, connectionContext)

                // Set up the consumer
                String consumerTag = channel.basicConsume(
                    queue,
                    configuration.autoAck == AutoAck.ALWAYS,
                    consumer
                )

                // Store the consumer tag
                consumer.consumerTag = consumerTag

                // Store the consumer
                consumers << consumer
            }
        }
        else {
            // Log our intentions
            log.debug("starting consumer '${getId()}' on connection '${connectionContext.id}'")

            // Create the channel
            Channel channel = connectionContext.createChannel()

            // Create a queue
            String queue = channel.queueDeclare().queue
            if (!configuration.binding || configuration.binding instanceof String) {
                channel.queueBind(queue, configuration.exchange, configuration.binding ?: '')
            }
            else if (configuration.binding instanceof Map) {
                channel.queueBind(queue, configuration.exchange, '', configuration.binding + ['x-match': configuration.match])
            }

            // Set the QOS
            channel.basicQos(configuration.prefetchCount)

            // Create the rabbit consumer object
            RabbitConsumer consumer = new RabbitConsumer(channel, this, connectionContext)

            // Set up the consumer
            String consumerTag = channel.basicConsume(
                queue,
                configuration.autoAck == AutoAck.ALWAYS,
                consumer
            )

            // Store the consumer tag
            consumer.consumerTag = consumerTag

            // Store the consumer
            consumers << consumer
        }
    }

    /**
     * Closes all channels and clears all consumers.
     */
    @Override
    void stop() {
        if (!consumers.size()) {
            return
        }
        consumers.each {
            if (it.channel.isOpen()) {
                it.channel.basicCancel(it.consumerTag)
                it.channel.close()
            }
        }
        consumers.clear()
        log.debug("stopped consumer '${getId()}' on connection '${getConnectionName()}'")

    }

    /**
     * Processes and delivers an incoming message to the consumer.
     *
     * @param context
     */
    private void deliverMessage(MessageContext context) {
        Object response
        try {
            // Process the message
            response = handoffMessage(context)
        }
        catch (Throwable e) {
            log.error("unexpected exception ${e.getClass()} encountered in the rabbit consumer associated with handler ${getId()}", e)
        }

        if (context.properties.replyTo && response != null) {
            try {
                // If a response was given and a replyTo is set, send the message back
                if (context.properties.replyTo && response) {
                    rabbitMessagePublisher.send {
                        channel = context.channel
                        routingKey = context.properties.replyTo
                        correlationId = context.properties.correlationId
                        delegate.body = response
                    }
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
     * @param context
     * @return Any returned value from the handler.
     */
    private Object handoffMessage(MessageContext context) {
        // Get the configuration
        ConsumerConfiguration configuration = getConfiguration()

        // Track whether the handler is MessageContext only
        boolean contextOnly = false

        // Convert the message body
        Object converted = convertMessage(context)

        // Find a valid handler
        Method method = getHandlerMethodForType(converted.getClass())

        // If no method is found, attempt to find the MessageContext handler
        if (!method) {
            method = getHandlerWithSignature([MessageContext])
            if (method) {
                contextOnly = true
            }
        }

        // Confirm that there is a handler defined to handle our message.
        if (!method) {
            // Reject the message
            if (configuration.getAutoAck() == AutoAck.POST) {
                context.channel.basicReject(context.envelope.deliveryTag, configuration.getRetry())
            }
            log.error("${getId()} does not have a message handler defined to handle class type ${converted.getClass()}")
            return
        }

        // Open a session
        openSession()

        // Call the received message callback
        onReceive(context)

        // Pass off the message
        try {
            // Start the transaction if requested
            if (configuration.getTransacted()) {
                context.channel.txSelect()
            }

            // Invoke the handler
            Object response
            if (contextOnly) {
                response = consumer."${RABBIT_HANDLER_NAME}"(context)
            }
            else if (method.parameterTypes.size() == 2) {
                response = consumer."${RABBIT_HANDLER_NAME}"(converted, context)
            }
            else {
                response = consumer."${RABBIT_HANDLER_NAME}"(converted)
            }

            // Ack the message
            if (configuration.getAutoAck() == AutoAck.POST) {
                context.channel.basicAck(context.envelope.deliveryTag, false)
            }

            // Commit the transaction if requested
            if (configuration.getTransacted()) {
                context.channel.txCommit()
            }

            // Call the success callback
            onSuccess(context)

            return response
        }
        catch (Throwable e) {
            // Rollback the transaction
            if (configuration.getTransacted()) {
                context.channel.txRollback()
            }

            // Reject the message, optionally submitting for requeue
            if (configuration.getAutoAck() == AutoAck.POST) {
                context.channel.basicReject(context.envelope.deliveryTag, configuration.getRetry())
            }

            // Log the error
            if (configuration.getTransacted()) {
                log.error("transaction rolled back due to unhandled exception ${e.getClass().name} caught in RabbitMQ message handler for consumer ${getId()}", e)
            }
            else {
                log.error("unhandled exception ${e.getClass().name} caught in RabbitMQ message handler for consumer ${getId()}", e)
            }

            // Call the failure callback
            onFailure(context)

            return null
        }
        finally {
            // Call the complete callback
            onComplete(context)

            // Close the session
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
    private Object convertMessage(MessageContext context) {
        // Get the configuration
        ConsumerConfiguration configuration = getConfiguration()

        // Check if the consumers wants us to not convert
        if (configuration.getConvert() == MessageConvertMethod.DISABLED) {
            return context.body
        }

        // If a content-type this converter is aware of is given, respect it.
        if (context.properties.contentType) {
            try {
                return messageConverterManager.convertFromBytes(context.body, context.properties.contentType)
            }
            catch (MessageConvertException e) {
                // Continue
            }
        }

        // If no content-type was handled, the config may specify to stop
        if (configuration.getConvert() == MessageConvertMethod.HEADER) {
            return context.body
        }

        // Try all message converters
        try {
            return messageConverterManager.convertFromBytes(context.body)
        }
        catch (MessageConvertException e) {
            // Continue
        }

        // No converters worked, so fall back to the byte array
        return context.body
    }

    /**
     * Determines if there is a message handler defined that will accommodate
     * a specific body class type.
     *
     * @param requested
     * @return
     */
    private Method getHandlerMethodForType(Class requested) {
        // Check for long parameter list
        Method method = getHandlerWithSignature([requested, MessageContext])
        if (method) {
            return method
        }

        // Check for short parameter list
        method = getHandlerWithSignature([requested])
        if (method) {
            return method
        }

        return null
    }

    /**
     * Attempts to locate a message handler that will accept the given object types.
     *
     * @param requested
     * @return
     */
    private Method getHandlerWithSignature(List<Class> requested) {
        // Get a list of methods that match the handler name
        List<Method> methods = consumer.class.getDeclaredMethods().findAll { it.name == RABBIT_HANDLER_NAME }

        // Find a matching method
        return methods.find { method ->
            // Get the method signature
            List<Class> signature = method.parameterTypes

            // Ensure we get the right number of parameters
            if (signature.size() != requested.size()) {
                return false
            }

            // Ensure each parameter is assignable
            for (int i = 0; i < signature.size(); i++) {
                if (!signature[i].isAssignableFrom(requested[i])) {
                    return false
                }
            }

            return true
        }
    }

    /**
     * Initiates one of the callback methods if the method exists.
     *
     * @param methodName
     * @param context
     */
    private void doCallback(String methodName, MessageContext context) {
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
    private void onReceive(MessageContext context) {
        doCallback(CONSUME_ON_RECEIVE_METHOD_NAME, context)
    }

    /**
     * Initiates the "on success" callback.
     *
     * @param context
     */
    private void onSuccess(MessageContext context) {
        doCallback(CONSUME_ON_SUCCESS_METHOD_NAME, context)
    }

    /**
     * Initiates the "on failure" callback.
     *
     * @param context
     */
    private void onComplete(MessageContext context) {
        doCallback(CONSUME_ON_COMPLETE_METHOD_NAME, context)
    }

    /**
     * Initiates the "on complete" callback.
     *
     * @param context
     */
    private void onFailure(MessageContext context) {
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
}
