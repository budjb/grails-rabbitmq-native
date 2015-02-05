package com.budjb.rabbitmq

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.List
import java.util.Map

import org.apache.log4j.Logger
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClass
import org.springframework.context.ApplicationContext

import com.budjb.rabbitmq.connection.ConnectionContext
import com.budjb.rabbitmq.converter.MessageConvertMethod
import com.budjb.rabbitmq.converter.MessageConverterManager
import com.budjb.rabbitmq.exception.MessageConvertException
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import com.rabbitmq.client.impl.recovery.AutorecoveringChannel

@SuppressWarnings("unchecked")
class RabbitConsumerAdapter {
    /**
     * Builder to simplify creating adapters.
     */
    public static class RabbitConsumerAdapterBuilder {
        Object consumer
        GrailsApplication grailsApplication
        RabbitContext rabbitContext
        MessageConverterManager messageConverterManager
        Object persistenceInterceptor

        /**
         * Returns a new rabbit consumer adapter instance.
         *
         * @param closure
         * @return
         */
        public RabbitConsumerAdapter build(Closure closure) {
            closure.delegate = this
            closure.resolveStrategy = Closure.OWNER_FIRST
            closure.run()

            return new RabbitConsumerAdapter(
                consumer,
                grailsApplication,
                rabbitContext,
                messageConverterManager,
                persistenceInterceptor,
            )
        }
    }

    /**
     * Consumer object used to receive messages from the RabbitMQ library.
     */
    protected class RabbitConsumer extends DefaultConsumer {
        /**
         * Consumer adapter containing the context for this consumer.
         */
        public RabbitConsumerAdapter adapter

        /**
         * Connection context associated with the consumer.
         */
        ConnectionContext connectionContext

        /**
         * Constructs an instance of a consumer.
         *
         * @param channel
         * @param adapter
         */
        private RabbitConsumer(Channel channel, RabbitConsumerAdapter adapter, ConnectionContext connectionContext) {
            // Run the parent
            super(channel)

            // Store the adapter
            this.adapter = adapter

            // Store the connection context
            this.connectionContext = connectionContext
        }

        /**
         * Passes delivery of a message back to the adapter for processing.
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

            // Hand off the message to the adapter.
            RabbitConsumerAdapter.this.deliverMessage(context)
        }
    }

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
     * Logger.
     */
    Logger log = Logger.getLogger(RabbitConsumerAdapter)

    /**
     * Grails application bean.
     */
    GrailsApplication grailsApplication

    /**
     * Consumer bean.
     */
    Object consumer

    /**
     * The consumer's configuration.
     */
    ConsumerConfiguration configuration

    /**
     * Rabbit context.
     */
    RabbitContext rabbitContext

    /**
     * List of active rabbit consumers.
     */
    List<RabbitConsumer> consumers = []

    /**
     * Persistence intercepter for Hibernate session handling.
     */
    Object persistenceInterceptor

    /**
     * Message converter manager.
     */
    MessageConverterManager messageConverterManager

    /**
     * Constructor.
     *
     * @param clazz
     * @param grailsApplication
     */
    public RabbitConsumerAdapter(Object consumer, GrailsApplication grailsApplication, RabbitContext rabbitContext, MessageConverterManager messageConverterManager, Object persistenceInterceptor) {
        this.consumer = consumer
        this.grailsApplication = grailsApplication
        this.rabbitContext = rabbitContext
        this.messageConverterManager = messageConverterManager
        this.persistenceInterceptor = persistenceInterceptor
    }

    /**
     * Retrieve the name of the connection the consumer belongs to.
     *
     * @return
     */
    public String getConnectionName() {
        return getConfiguration().getConnection()
    }

    /**
     * Finds and returns a consumer's central configuration, or null if it isn't defined.
     *
     * @return
     */
    protected Map getCentralConfiguration() {
        // Attempt to find a configuration path that matches the class name
        def configuration = grailsApplication.config.rabbitmq.consumers."${getConsumerName()}"

        // Ensure it exists and is a map
        if (!configuration || !Map.class.isAssignableFrom(configuration.getClass())) {
            return null
        }

        return configuration
    }

    /**
     * Finds and returns a consumer's local configuration, or null if it doesn't exist.
     *
     * @return
     */
    protected Map getLocalConfiguration() {
        // Ensure the config field is set and is static
        try {
            Field field = consumer.class.getDeclaredField(RABBIT_CONFIG_NAME)
            if (!Modifier.isStatic(field.modifiers)) {
                return null
            }
        }
        catch (NoSuchFieldException e) {
            return null
        }

        // Ensure the config field is a map
        if (!Map.class.isAssignableFrom(consumer."${RABBIT_CONFIG_NAME}".getClass())) {
            return null
        }

        return consumer."${RABBIT_CONFIG_NAME}"
    }

    /**
     * Finds and returns the consumer's configuration, or null if one is not defined.
     *
     * @return
     */
    public ConsumerConfiguration getConfiguration() {
        if (!configuration) {
            configuration = new ConsumerConfiguration(getLocalConfiguration() ?: getCentralConfiguration())
        }
        return configuration
    }

    /**
     * Determines if a handler is a RabbitMQ consumer.
     *
     * @return
     */
    public boolean isConsumerValid() {
        // Get the configuration
        ConsumerConfiguration configuration = getConfiguration()

        // Check if there is either a local or central configuration
        if (!configuration) {
            return false
        }

        // Check if we find any handler defined
        if (!consumer.class.getDeclaredMethods().any { it.name == RABBIT_HANDLER_NAME }) {
            return false
        }

        // Make sure a queue or an exchange was specified
        if (!configuration.getQueue() && !configuration.getExchange()) {
            log.warn("RabbitMQ configuration for consumer '${getConsumerName()}' is missing a queue or an exchange")
            return false
        }

        // Make sure that only a queue or an exchange was specified
        if (configuration.getQueue() && configuration.getExchange()) {
            log.warn("RabbitMQ configuration for consumer '${getConsumerName()}' can not have both a queue and an exchange")
            return false
        }

        return true
    }

    /**
     * Returns the consumer's short name.
     *
     * @return
     */
    public String getConsumerName() {
        return consumer.getClass().getSimpleName()
    }

    /**
     * Starts a consumer against a handler.
     *
     * @param connection Connection to the RabbitMQ server.
     * @param handler Handler object to wrap a RabbitMQ consumer around.
     * @return A list of channels that were created for the consumer.
     */
    public void start() {
        // Ensure the object is a consumer
        if (!isConsumerValid()) {
            log.warn("not registering '${getConsumerName()}' as a RabbitMQ message consumer because it is not properly configured")
            return
        }

        // Ensure there are no active consumers
        if (consumers.size()) {
            throw new IllegalStateException("attempted to start consumers but active consumers already exist")
        }

        // Get the configuration
        ConsumerConfiguration configuration = getConfiguration()

        // Get the connection context
        ConnectionContext connectionContext = rabbitContext.getConnection(configuration.getConnection())

        // Ensure we have a connection
        if (!connectionContext) {
            log.warn("not registering '${getConsumerName()}' as a RabbitMQ message consumer because a suitable connection could not be found")
            return
        }

        // Start the consumers
        if (configuration.queue) {
            // Log our intentions
            log.debug("registering consumer '${getConsumerName()}' as a RabbitMQ consumer on connection '${connectionContext.getConfiguration().getName()}' with ${configuration.getConsumers()} consumer(s)")

            // Create all requested consumer instances
            configuration.getConsumers().times {
                // Create the channel
                Channel channel = connectionContext.createChannel()

                // Determine the queue
                String queue = configuration.getQueue()

                // Set the QOS
                channel.basicQos(configuration.getPrefetchCount())

                // Create the rabbit consumer object
                RabbitConsumer consumer = new RabbitConsumer(channel, this, connectionContext)

                // Set up the consumer
                channel.basicConsume(
                    queue,
                    configuration.getAutoAck() == AutoAck.ALWAYS,
                    consumer
                )

                // Store the consumer
                consumers << consumer
            }
        }
        else {
            // Log our intentions
            log.debug("registering consumer '${getConsumerName()}' on connection '${connectionContext.getConfiguration().getName()}' as a RabbitMQ subscriber")

            // Create the channel
            Channel channel = connectionContext.createChannel()

            // Create a queue
            String queue = channel.queueDeclare().queue
            if (!configuration.getBinding() || configuration.getBinding() instanceof String) {
                channel.queueBind(queue, configuration.getExchange(), configuration.getBinding() ?: '')
            }
            else if (configuration.getBinding() instanceof Map) {
                if (!(configuration.getMatch() in ['any', 'all'])) {
                    log.warn("not starting consumer '${getConsumerName()}' since the match property was not set or not one of (\"any\", \"all\")")
                    return
                }
                channel.queueBind(queue, configuration.getExchange(), '', configuration.getBinding() + ['x-match': configuration.getMatch()])
            }

            // Set the QOS
            channel.basicQos(configuration.getPrefetchCount())

            // Create the rabbit consumer object
            RabbitConsumer consumer = new RabbitConsumer(channel, this, connectionContext)

            // Set up the consumer
            channel.basicConsume(
                queue,
                configuration.autoAck == AutoAck.ALWAYS,
                consumer
            )

            // Store the consumer
            consumers << consumer
        }
    }

    /**
     * Processes and delivers an incoming message to the consumer.
     *
     * @param context
     */
    private void deliverMessage(MessageContext context) {
        try {
            // Process the message
            Object response = processMessage(context)

            // If a response was given and a replyTo is set, send the message back
            // TODO: change to rabbitMessagePublisher!
            if (context.properties.replyTo && response) {
                new RabbitMessageBuilder(context.channel).send {
                    routingKey = context.properties.replyTo
                    correlationId = context.properties.correlationId
                    delegate.body = response
                }
            }
        }
        catch (Exception e) {
            log.error("unexpected exception ${e.getClass()} encountered in the rabbit consumer associated with handler ${getConsumerName()}", e)
        }
    }

    /**
     * Processes the message and hands it off to the handler.
     *
     * @param context
     * @return Any returned value from the handler.
     */
    private Object processMessage(MessageContext context) {
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
            log.error("${getConsumerName()} does not have a message handler defined to handle class type ${converted.getClass()}")
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
        catch (Exception e) {
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
                log.error("transaction rolled back due to unhandled exception ${e.getClass().name} caught in RabbitMQ message handler for consumer ${getConsumerName()}", e)
            }
            else {
                log.error("unhandled exception ${e.getClass().name} caught in RabbitMQ message handler for consumer ${getConsumerName()}", e)
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
        persistenceInterceptor?.init()
    }

    /**
     * Closes the bound Hibernate session if Hibernate is present.
     */
    protected void closeSession() {
        // Flush the session
        persistenceInterceptor?.flush()

        // Close the session
        persistenceInterceptor?.destroy()
    }
}
