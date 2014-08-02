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

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import com.rabbitmq.client.impl.recovery.AutorecoveringChannel
import groovy.json.JsonSlurper
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import org.apache.log4j.Logger
import org.codehaus.groovy.grails.commons.GrailsClass
import grails.util.Holders

@SuppressWarnings("unchecked")
class RabbitConsumer extends DefaultConsumer {
    /**
     * Logger
     */
    private static Logger log = Logger.getLogger(RabbitConsumer)

    /**
     * Name of the handler method a consumer is expected to define.
     */
    static final String RABBIT_HANDLER_NAME = 'handleMessage'

    /**
     * Name of the configuration variable a consumer is expected to define.
     */
    static final String RABBIT_CONFIG_NAME = 'rabbitConfig'

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
     * Handler GrailsClass.
     */
    private GrailsClass handler

    /**
     * Rabbit context.
     */
    RabbitContext rabbitContext

    /**
     * Configuration provided by the handler for this consumer.
     */
    private ConsumerConfiguration configuration

    /**
     * Connection context associated with this consumer.
     */
    private ConnectionContext connectionContext

    /**
     * Retrieve the name of the connection the consumer belongs to.
     *
     * @param clazz
     * @return
     */
    public static String getConnectionName(GrailsClass clazz) {
        return getConnectionName(clazz.clazz)
    }

    /**
     * Retrieve the name of the connection the consumer belongs to.
     *
     * @param clazz
     * @return
     */
    public static String getConnectionName(Class clazz) {
        return (clazz)."${RABBIT_CONFIG_NAME}"['connection'] ?: null
    }

    /**
     * Determines if a handler is a RabbitMQ consumer.
     *
     * @param clazz
     * @return
     */
    public static boolean isConsumer(GrailsClass clazz) {
        return isConsumer(clazz.clazz)
    }

    /**
     * Determines if a handler is a RabbitMQ consumer.
     *
     * @param clazz
     * @return
     */
    public static boolean isConsumer(Class clazz) {
        // Ensure the config field is set
        try {
            Field field = clazz.getDeclaredField(RABBIT_CONFIG_NAME)
            if (!Modifier.isStatic(field.modifiers)) {
                return false
            }
        }
        catch (NoSuchFieldException e) {
            return false
        }

        // Ensure the config field is a map
        if (!Map.class.isAssignableFrom(clazz."${RABBIT_CONFIG_NAME}".getClass())) {
            return false
        }

        // Check if we find any handler defined
        if (clazz.getDeclaredMethods().any { it.name == RABBIT_HANDLER_NAME }) {
            return true
        }

        return false
    }

    /**
     * Starts a consumer against a handler.
     *
     * @param connection Connection to the RabbitMQ server.
     * @param handler Handler object to wrap a RabbitMQ consumer around.
     * @return A list of channels that were created for the consumer.
     */
    public static List<Channel> startConsumer(ConnectionContext connectionContext, GrailsClass handler) {
        // Check if the object wants to be a consumer
        if (!RabbitConsumer.isConsumer(handler)) {
            return []
        }

        // Load the rabbit config properties into a configuration holder
        ConsumerConfiguration config = new ConsumerConfiguration(handler.getPropertyValue(RABBIT_CONFIG_NAME))

        // Make sure a queue or an exchange was specified
        if (!config.queue && !config.exchange) {
            log.warn("RabbitMQ configuration for consumer '${handler.shortName}' is missing a queue or an exchange")
            return []
        }

        // Make sure that only a queue or an exchange was specified
        if (config.queue && config.exchange) {
            log.warn("RabbitMQ configuration for consumer '${handler.shortName}' can not have both a queue and an exchange")
            return []
        }

        // Store our channels so the caller can keep track of them
        List<Channel> channels = []

        // Start the consumers
        if (config.queue) {
            log.debug("registering consumer '${handler.shortName}' as a RabbitMQ consumer on connection '${connectionContext.name}' with ${config.consumers} consumer(s)")
            config.consumers.times {
                // Create the channel
                Channel channel = connectionContext.connection.createChannel()

                // Add listeners
                channel.addShutdownListener(new ChannelShutdownListener())
                if (channel instanceof AutorecoveringChannel) {
                    ((AutorecoveringChannel)channel).addRecoveryListener(new AutorecoveryListener())
                }

                // Determine the queue
                String queue = config.queue

                // Set the QOS
                channel.basicQos(config.prefetchCount)

                // Set up the consumer
                channel.basicConsume(
                    queue,
                    config.autoAck == AutoAck.ALWAYS,
                    new RabbitConsumer(channel, config, connectionContext, handler)
                )

                // Store the channel
                channels << channel
            }
        }
        else {
            // Log it
            log.debug("registering consumer '${handler.shortName}' on connection '${connectionContext.name}' as a RabbitMQ subscriber")

            // Create the channel
            Channel channel = connectionContext.connection.createChannel()

            // Create a queue
            String queue = channel.queueDeclare().queue
            if (!config.binding || config.binding instanceof String) {
                channel.queueBind(queue, config.exchange, config.binding ?: '')
            }
            else if (config.binding instanceof Map) {
                if (!(config.match in ['any', 'all'])) {
                    log.warn("not starting consumer '${handler.shortName}' since the match property was not set or not one of (\"any\", \"all\")")
                    return
                }
                channel.queueBind(queue, config.exchange, '', config.binding + ['x-match': config.match])
            }

            // Set the QOS
            channel.basicQos(config.prefetchCount)

            // Set up the consumer
            channel.basicConsume(
                queue,
                config.autoAck == AutoAck.ALWAYS,
                new RabbitConsumer(channel, config, connectionContext, handler)
            )

            // Store the channel
            channels << channel
        }

        return channels
    }

    /**
     * Constructs an instance of a consumer.
     *
     * @param channel
     * @param grailsClass
     */
    public RabbitConsumer(Channel channel, ConsumerConfiguration configuration, ConnectionContext connectionContext, GrailsClass handler) {
        // Run the parent
        super(channel)

        // Grab the rabbit context object
        this.rabbitContext = Holders.grailsApplication.mainContext.getBean('rabbitContext')

        // Store the handler this consumer is acting on behalf of
        this.handler = handler

        // Store the configuration
        this.configuration = configuration

        // Store the connection context
        this.connectionContext = connectionContext
    }

    /**
     * Passes delivery of a message to the handler registered with this consumer instance.
     *
     * @param consumerTag
     * @param envelope
     * @param properties
     * @param body
     */
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
        // Wrap up the parameters into a context
        MessageContext context = new MessageContext(
            channel: channel,
            consumerTag: consumerTag,
            envelope: envelope,
            properties: properties,
            body: body,
            connectionContext: connectionContext
        )

        // Process and hand off the message to the consumer
        try {
            // Process the message
            Object response = processMessage(context)

            // If a response was given and a replyTo is set, send the message back
            if (context.properties.replyTo && response) {
                new RabbitMessageBuilder(context.channel).send {
                    routingKey = context.properties.replyTo
                    correlationId = context.properties.correlationId
                    delegate.body = response
                }
            }
        }
        catch (Exception e) {
            log.error("unexpected exception ${e.getClass()} encountered in the rabbit consumer associated with handler ${handler.shortName}", e)
        }
    }

    /**
     * Processes the message and hands it off to the handler.
     *
     * @param context
     * @return Any returned value from the handler.
     */
    private Object processMessage(MessageContext context) {
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
            if (configuration.autoAck == AutoAck.POST) {
                context.channel.basicReject(context.envelope.deliveryTag, configuration.retry)
            }
            log.error("${handler.shortName} does not have a message handler defined to handle class type ${converted.getClass()}")
            return
        }

        // Get the handler bean
        Object handlerBean = getHandlerBean()

        // Open a session
        openSession()

        // Call the received message callback
        onReceive(handlerBean, context)

        // Pass off the message
        try {
            // Start the transaction if requested
            if (configuration.transacted) {
                context.channel.txSelect()
            }

            // Invoke the handler
            Object response
            if (contextOnly) {
                response = handlerBean."${RABBIT_HANDLER_NAME}"(context)
            }
            else if (method.parameterTypes.size() == 2) {
                response = handlerBean."${RABBIT_HANDLER_NAME}"(converted, context)
            }
            else {
                response = handlerBean."${RABBIT_HANDLER_NAME}"(converted)
            }

            // Ack the message
            if (configuration.autoAck == AutoAck.POST) {
                channel.basicAck(context.envelope.deliveryTag, false)
            }

            // Commit the transaction if requested
            if (configuration.transacted) {
                context.channel.txCommit()
            }

            // Call the success callback
            onSuccess(handlerBean, context)

            return response
        }
        catch (Exception e) {
            // Rollback the transaction
            if (configuration.transacted) {
                context.channel.txRollback()
            }

            // Reject the message, optionally submitting for requeue
            if (configuration.autoAck == AutoAck.POST) {
                channel.basicReject(context.envelope.deliveryTag, configuration.retry)
            }

            // Log the error
            if (configuration.transacted) {
                log.error("transaction rolled back due to unhandled exception ${e.getClass().name} caught in RabbitMQ message handler for consumer ${handler.shortName}", e)
            }
            else {
                log.error("unhandled exception ${e.getClass().name} caught in RabbitMQ message handler for consumer ${handler.shortName}", e)
            }

            // Call the failure callback
            onFailure(handlerBean, context)

            return null
        }
        finally {
            // Call the complete callback
            onComplete(handlerBean, context)

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
        // Check if the consumers wants us to not convert
        if (configuration.convert == MessageConvertMethod.DISABLED) {
            return context.body
        }

        // If a content-type this converter is aware of is given, respect it.
        if (context.properties.contentType) {
            // Find a converter
            List<MessageConverter> converters = rabbitContext.messageConverters.findAll { it.contentType == context.properties.contentType }

            // If converters are found and it can convert to its type, allow it to do so
            for (MessageConverter converter in converters) {
                Object converted = attemptConversion(converter, context)
                if (converted != null) {
                    return converted
                }
            }
        }

        // If no content-type was handled, the config may specify to stop
        if (configuration.convert == MessageConvertMethod.HEADER) {
            return context.body
        }

        // Iterate through converters until we have success
        for (MessageConverter converter in rabbitContext.messageConverters) {
            Object converted = attemptConversion(converter, context)
            if (converted != null) {
                return converted
            }
        }

        // No converters worked, so fall back to the byte array
        return context.body
    }

    /**
     * Attempts to convert a message with the given converter.
     *
     * @param converter
     * @param context
     * @return
     */
    public Object attemptConversion(MessageConverter converter, MessageContext context) {
        // Skip if the converter can't convert the message from a byte array
        if (!converter.canConvertTo()) {
            return null
        }

        try {
            // Convert the message
            Object converted = converter.convertTo(context.body)

            // If conversion worked and a handler is defined for the type, we're done
            if (converted != null && getHandlerMethodForType(converted.getClass())) {
                return converted
            }
        }
        catch (Exception e) {
            log.error("unhandled exception caught from message converter ${converter.class.simpleName}", e)
        }

        return null
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
     * Initiates one of the callback methods if the method exists.
     *
     * @param methodName
     * @param bean
     * @param context
     */
    private void doCallback(String methodName, Object bean, MessageContext context) {
        if (!bean.class.metaClass.methods.find { it.name == methodName }) {
            return
        }

        bean."${methodName}"(context)
    }

    /**
     * Initiates the "on received" callback.
     *
     * @param bean
     * @param context
     */
    private void onReceive(Object bean, MessageContext context) {
        doCallback(CONSUME_ON_RECEIVE_METHOD_NAME, bean, context)
    }

    /**
     * Initiates the "on success" callback.
     *
     * @param bean
     * @param context
     */
    private void onSuccess(Object bean, MessageContext context) {
        doCallback(CONSUME_ON_SUCCESS_METHOD_NAME, bean, context)
    }

    /**
     * Initiates the "on failure" callback.
     *
     * @param bean
     * @param context
     */
    private void onComplete(Object bean, MessageContext context) {
        doCallback(CONSUME_ON_COMPLETE_METHOD_NAME, bean, context)
    }

    /**
     * Initiates the "on complete" callback.
     *
     * @param bean
     * @param context
     */
    private void onFailure(Object bean, MessageContext context) {
        doCallback(CONSUME_ON_FAILURE_METHOD_NAME, bean, context)
    }

    /**
     * Attempts to locate a message handler that will accept the given object types.
     *
     * @param requested
     * @return
     */
    private Method getHandlerWithSignature(List<Class> requested) {
        // Get a list of methods that match the handler name
        List<Method> methods = handler.clazz.getDeclaredMethods().findAll { it.name == RABBIT_HANDLER_NAME }

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
     * Returns the bean for the handler Grails class.
     *
     * @return
     */
    protected Object getHandlerBean() {
        return Holders.applicationContext.getBean(handler.fullName)
    }

    /**
     * Binds a Hibernate session to the current thread if Hibernate is present.
     */
    protected void openSession() {
        // Get the persistence interceptor
        def persistenceInterceptor
        try {
            persistenceInterceptor = Holders.applicationContext.getBean('persistenceInterceptor')
        }
        catch (NoSuchBeanDefinitionException) {
            return
        }

        // Bind a session
        persistenceInterceptor.init()
    }

    /**
     * Closes the bound Hibernate session if Hibernate is present.
     */
    protected void closeSession() {
        // Get the persistence interceptor
        def persistenceInterceptor
        try {
            persistenceInterceptor = Holders.applicationContext.getBean('persistenceInterceptor')
        }
        catch (NoSuchBeanDefinitionException) {
            return
        }

        // Flush the session
        persistenceInterceptor.flush()

        // Close the session
        persistenceInterceptor.destroy()
    }
}
