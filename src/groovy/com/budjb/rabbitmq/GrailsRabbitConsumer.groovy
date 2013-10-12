package com.budjb.rabbitmq

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import groovy.json.JsonSlurper
import java.lang.reflect.Method
import org.apache.log4j.Logger
import org.codehaus.groovy.grails.commons.GrailsClass

class GrailsRabbitConsumer extends DefaultConsumer {
    /**
     * Logger
     */
    private static Logger log = Logger.getLogger(GrailsRabbitConsumer)

    /**
     * Name of the handler method a listener is expected to define.
     */
    static final String RABBIT_HANDLER_NAME = 'handleMessage'

    /**
     * Name of the configuration variable a listener is expected to define.
     */
    static final String RABBIT_CONFIG_NAME = 'rabbitConfig'

    /**
     * Instance of the service's GrailsClass associated with this listener.
     */
    private GrailsClass service

    /**
     * Configuration provided by the service for this listener.
     */
    private ConsumerConfiguration configuration

    /**
     * Determines if a service is a RabbitMQ listener.
     *
     * @param grailsClass
     * @return
     */
    public static boolean isConsumer(GrailsClass grailsClass) {
        // Check for the existence and type of the rabbit config static variable
        if (!grailsClass.hasProperty(RABBIT_CONFIG_NAME) || !(grailsClass.getPropertyValue(RABBIT_CONFIG_NAME) instanceof Map)) {
            return false
        }

        // Check for the existence of the handleMessage method
        if (!grailsClass.metaClass.methods.any { it.name == RABBIT_HANDLER_NAME }) {
            return false
        }

        return true
    }

    /**
     * Constructs an instance of a consumer.
     *
     * @param channel
     * @param grailsClass
     */
    public GrailsRabbitConsumer(Channel channel, ConsumerConfiguration configuration, GrailsClass service) {
        // Run the parent
        super(channel)

        // Store the service this consumer is acting on behalf of
        this.service = service

        // Store the configuration
        this.configuration = configuration
    }

    /**
     * Passes delivery of a message to the service registered with this consumer instance.
     *
     * @param consumerTag
     * @param envelope
     * @param properties
     * @param body
     */
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
        // Wrap up the parameters into a context
        MessageContext context = new MessageContext(
            consumerTag: consumerTag,
            envelope: envelope,
            properties: properties,
            body: body
        )

        // Convert the message body
        Object converted = convertMessage(context)

        // Confirm that there is a handler defined to handle our message.
        // TODO: remove or refactor this try/catch.  It's debug for now.
        try {
            if (!isHandlerTypeDefined(converted.class)) {
                // TODO: can we stick this message back on the bus?
                // TODO: better error handling here.  The rabbit service apparently
                //       catches all errors and silently discards them
                log.error("${service.name} does not have a message handler defined to handle class type ${converted.class}")
                return
            }
            // Get the instance of the service
            Object serviceInstance = service.referenceInstance

            // Invoke the method
            try {
                serviceInstance."${RABBIT_HANDLER_NAME}"(converted, context)
            }
            catch (Exception e) {
                // TODO: better error handling here
                log.error("Unhandled exception caught in the consumer for service ${service.name}.")
                return
            }
        } catch (Exception e) {
            log.error("(${e.class}) ${e.message}")
            e.stackTrace.each {
                log.error(it)
            }
            return
        }
    }

    /**
     * Attempts to convert the body of the incoming message from a byte array.
     * The output of this method is dependent on the listener's configuration,
     * the content-type of the message, and the existence of an appropriately
     * defined handler for the converted type.
     *
     * @param context
     * @return
     */
    private Object convertMessage(MessageContext context) {
        // Check if the listeners wants us to not convert
        if (configuration.convert == false) {
            return context.body
        }

        // Check for forced JSON conversion
        if (configuration.alwaysConvertJson) {
            return convertJson(context)
        }

        // Attempt to convert the message based on the content type
        // TODO: we may have to add more content types here to
        // supplement the base content types.  For example, there
        // are at least 3 JSON content types I know of.
        switch (context.properties.contentType) {
            // Handle strings
            case 'text/plain':
                return convertString(context)
                break

            // Handle JSON
            case 'application/json':
                return convertJson(context)
                break

            default:
                return context.body
        }
    }

    /**
     * Attempts to locate a handler for JSON types, and converts the
     * message body to JSON.  This converter will attempt to convert
     * to a string on failure to convert to JSON.
     *
     * @param context
     * @return
     */
    private Object convertJson(MessageContext context) {
        // First check whether Map or List type handlers are defined
        if (!isHandlerTypeDefined(Map.class) && !isHandlerTypeDefined(List.class)) {
            return convertString(context)
        }

        // Convert the body to a string.
        // If it fails, just return the byte array since convertString won't work.
        String raw
        try {
            raw = new String(context.body)
        }
        catch (Exception e) {
            return context.body
        }

        // Convert the raw string to JSON.
        Object json
        try {
            json = new JsonSlurper().parseText(raw)
        }
        catch (Exception e) {
            return convertString(context.body)
        }

        // Finally, determine if we really have the correct handler defined.
        if (!isHandlerTypeDefined(json.class)) {
            return convertString(context.body)
        }

        return json
    }

    /**
     * Attempts to locate a handler for String types and converts the message
     * to a String.  The converter will fall back to the byte array on failure.
     *
     * @param context
     * @return
     */
    private Object convertString(MessageContext context) {
        // Fall back to the byte array if a String handler is not defined.
        if (!isHandlerTypeDefined(String.class)) {
            return context.body
        }

        // Attempt to return the string
        try {
            return new String(context.body)
        }
        catch (Exception e) {
            return context.body
        }
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
        List<Method> methods = service.referenceInstance.class.getDeclaredMethods().findAll { it.name == 'handleMessage' }

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
            if (!signature[1].isAssignableFrom(MessageContext.class)) {
                return false
            }

            // Finally, determine if the first parameter will handle our requested type
            return signature[0].isAssignableFrom(requested)
        }
    }
}
