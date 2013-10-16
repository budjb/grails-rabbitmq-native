package com.budjb.rabbitmq

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import groovy.json.JsonSlurper
import java.lang.reflect.Method
import org.apache.log4j.Logger
import org.codehaus.groovy.grails.commons.GrailsClass
import grails.util.Holders

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
        // Check for the existence and type of the rabbit config static variable
        if (!clazz.metaClass.properties.any { it.name == RABBIT_CONFIG_NAME && it.type.isAssignableFrom(Map) }) {
            return false
        }

        // Check for the existence of the handleMessage method
        if (!clazz.metaClass.methods.any { it.name == RABBIT_HANDLER_NAME }) {
            return false
        }

        return true
    }

    /**
     * Starts a consumer against a handler.
     *
     * @param connection Connection to the RabbitMQ server.
     * @param handler Handler object to wrap a RabbitMQ consumer around.
     * @return A list of channels that were created for the consumer.
     */
    public static List<Channel> startConsumer(Connection connection, GrailsClass handler) {
        // Check if the object wants to be a consumer
        if (!RabbitConsumer.isConsumer(handler)) {
            return []
        }

        // Load the rabbit config properties into a configuration holder
        ConsumerConfiguration config = new ConsumerConfiguration(handler.getPropertyValue(RABBIT_CONFIG_NAME))

        // Make sure a queue or an exchange was specified
        if (!config.queue && !config.exchange) {
            log.error("RabbitMQ configuration for consumer ${handler.shortName} is missing a queue or an exchange")
            return []
        }

        // Make sure that only a queue or an exchange was specified
        if (config.queue && config.exchange) {
            log.error("RabbitMQ configuration for consumer ${handler.shortName} can not have both a queue and an exchange")
            return []
        }

        // Store our channels so the caller can keep track of them
        List<Channel> channels = []

        // Start the consumers
        log.info("registering consumer ${handler.shortName} as a RabbitMQ consumer with ${config.consumers} consumer(s)")
        config.consumers.times {
            // Create the channel
            Channel channel = connection.createChannel()

            // Determine the queue
            String queue
            if (config.queue){
                queue = config.queue
            }
            else {
                queue = channel.queueDeclare().queue
                channel.queueBind(queue, config.exchange, config.routingKey)
            }

            // Set up the consumer
            channel.basicConsume(
                queue,
                config.autoAck == AutoAck.ALWAYS,
                new RabbitConsumer(channel, config, handler)
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
    public RabbitConsumer(Channel channel, ConsumerConfiguration configuration, GrailsClass handler) {
        // Run the parent
        super(channel)

        // Grab the rabbit context object
        this.rabbitContext = Holders.grailsApplication.mainContext.getBean('rabbitContext')

        // Store the handler this consumer is acting on behalf of
        this.handler = handler

        // Store the configuration
        this.configuration = configuration
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
            body: body
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
        // Convert the message body
        Object converted = convertMessage(context)

        // Confirm that there is a handler defined to handle our message.
        if (!isHandlerTypeDefined(converted.getClass())) {
            // Reject the message
            if (configuration.autoAck == AutoAck.POST) {
                context.channel.basicReject(context.envelope.deliveryTag, configuration.retry)
            }
            log.error("${handler.shortName} does not have a message handler defined to handle class type ${converted.getClass()}")
            return
        }

        // Pass off the message
        try {
            // Get the handler bean
            Object handlerBean = getHandlerBean()

            // Invoke the handler
            Object response = handlerBean."${RABBIT_HANDLER_NAME}"(converted, context)

            // Ack the message
            if (configuration.autoAck == AutoAck.POST) {
                channel.basicAck(context.envelope.deliveryTag, false)
            }

            return response
        }
        catch (Exception e) {
            // Reject the message, optionally submitting for requeue
            if (configuration.autoAck == AutoAck.POST) {
                channel.basicReject(context.envelope.deliveryTag, configuration.retry)
            }

            // Log the error
            log.error("unhandled exception ${e.getClass().name} caught from RabbitMQ message handler for consumer ${handler.shortName}", e)
            return null
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
            MessageConverter converter = rabbitContext.messageConverters.find { it.contentType == context.properties.contentType }

            // If a converter is found and it can convert to its type, allow it to do so
            if (converter) {
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
            if (converted != null && isHandlerTypeDefined(converted.getClass())) {
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
    private boolean isHandlerTypeDefined(Class requested) {
        // Get a list of methods that match the handler name
        List<Method> methods = handler.clazz.getDeclaredMethods().findAll { it.name == RABBIT_HANDLER_NAME }

        // Get a list of method parameter lists
        List<Class[]> signatures = methods*.parameterTypes

        // Determine if there are any method signatures that will
        // take our requested data type.
        return signatures.any { Class[] signature ->
            // The method should take 2 parameters.
            if (signature.size()  != 2) {
                return false
            }

            // Ensure that the second parameter takes a MessageContext.
            if (!signature[1].isAssignableFrom(MessageContext)) {
                return false
            }

            // Finally, determine if the first parameter will handle our requested type
            return signature[0].isAssignableFrom(requested)
        }
    }

    /**
     * Returns the bean for the handler grails class.
     *
     * @return
     */
    protected Object getHandlerBean() {
        return Holders.applicationContext.getBean(handler.fullName)
    }
}
