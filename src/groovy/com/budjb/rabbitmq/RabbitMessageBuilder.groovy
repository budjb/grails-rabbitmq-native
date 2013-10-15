package com.budjb.rabbitmq

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import com.rabbitmq.client.MessageProperties
import com.rabbitmq.client.RpcClient
import com.rabbitmq.client.ShutdownSignalException
import grails.converters.JSON
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.apache.log4j.Logger
import org.codehaus.groovy.grails.commons.GrailsApplication
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import com.budjb.rabbitmq.exception.MessageConvertException

class RabbitMessageBuilder {
    /**
     * Grails application.
     */
    public static GrailsApplication grailsApplication

    /**
     * Default timeout for RPC calls (5 seconds).
     */
    public static final int DEFAULT_TIMEOUT = 5000

    /**
     * Logger
     */
    protected static Logger log = Logger.getLogger(RabbitMessageBuilder)

    /**
     * Channel to publish messages through.
     */
    Channel channel

    /**
     * Routing key to send the message to.
     */
    String routingKey = ''

    /**
     * Exchange to send the message to.
     */
    String exchange = ''

    /**
     * RPC timeout, in milliseconds.
     */
    int timeout = DEFAULT_TIMEOUT

    /**
     * Message body.
     */
    Object body

    /**
     * Message headers.
     */
    Map headers = [:]

    /**
     * Content type.
     */
    String contentType

    /**
     * Content encoding.
     */
    String contentEncoding

    /**
     * Delivery mode (1 == non-persistent, 2 == persistent)
     */
    int deliveryMode

    /**
     * Priority.
     */
    int priority

    /**
     * Correlation id.
     */
    String correlationId

    /**
     * Queue to reply to.
     */
    String replyTo

    /**
     * Message expiration.
     */
    String expiration

    /**
     * Message ID.
     */
    String messageId

    /**
     * Message timestamp.
     */
    Calendar timestamp

    /**
     * Message type name.
     */
    String type

    /**
     * User ID.
     */
    String userId

    /**
     * Application ID.
     */
    String appId

    /**
     * Whether to auto-convert the reply payload.
     */
    boolean autoConvert = true

    /**
     * Constructor
     *
     * Loads the rabbit template bean registered from the grails plugin.
     */
    public RabbitMessageBuilder(Channel channel = null) {
        this.channel = channel
    }

    /**
     * Sends a message to the rabbit service.
     *
     * @throws IllegalArgumentException
     */
    protected void doSend() throws IllegalArgumentException{
        // Make sure an exchange or a routing key were provided
        if (!exchange && !routingKey) {
            throw new IllegalArgumentException("exchange and/or routing key required")
        }

        // Make sure a message was provided
        if (!this.body) {
            throw new IllegalArgumentException("body required")
        }

        // Build properties
        AMQP.BasicProperties properties = buildProperties()

        // Convert the object and create the message
        byte[] body = convertMessage(this.body)

        // Create a channel
        channel = RabbitContext.instance.connection.createChannel()

        // Send the message
        channel.basicPublish(exchange, routingKey, properties, body)

        // Close the channel
        channel.close()
    }

    /**
     * Sends a message to the rabbit service.
     *
     * @throws IllegalArgumentException
     */
    public void send() throws IllegalArgumentException {
        doSend()
    }

    /**
     * Sends a message to the rabbit service.
     *
     * @param closure
     * @throws IllegalArgumentException
     */
    public void send(Closure closure) throws IllegalArgumentException {
        // Run the closure
        run closure

        // Send the message
        doSend()
    }

    /**
     * Sends a message to the rabbit service.
     *
     * @param routingKey Routing key to send the message to.
     * @param body Message payload.
     * @throws IllegalArgumentException
     */
    public void send(String routingKey, Object body) throws IllegalArgumentException {
        // Set the params
        this.routingKey = routingKey
        this.body = body

        // Send the message
        doSend()
    }

    /**
     * Sends a message to the rabbit service.
     *
     * @param exchange Exchange to send the message to.
     * @param routingKey Routing key to send the message to.
     * @param body Message payload.
     * @throws IllegalArgumentException
     */
    public void send(String exchange, String routingKey, Object body) throws IllegalArgumentException {
        // Set the params
        this.exchange = exchange
        this.routingKey = routingKey
        this.body = body

        // Send the message
        doSend()
    }

    /**
     * Sends a message to the bus and waits for a reply, up to the "timeout" property.
     *
     * This method returns a Message object if autoConvert is set to false, or some
     * other object type (string, list, map) if autoConvert is true.
     *
     * The logic for the handler is based on the RPC handler found in spring's RabbitTemplate.
     *
     * @throws TimeoutException
     * @throws ShutdownSignalException
     * @throws IOException
     * @throws IllegalArgumentException
     * @return
     */
    protected Object doRpc() throws TimeoutException, ShutdownSignalException, IOException, IllegalArgumentException {
        // Make sure an exchange or a routing key were provided
        if (!exchange && !routingKey) {
            throw new IllegalArgumentException("exchange and/or routing key required")
        }

        // Make sure a message was provided
        if (!this.body) {
            throw new IllegalArgumentException("body required")
        }

        // Build properties
        AMQP.BasicProperties properties = buildProperties()

        // Convert the object and create the message
        byte[] body = convertMessage(this.body)

        // Create a channel for the message
        channel = RabbitContext.instance.connection.createChannel()

        // Set the reply queue
        properties.replyTo = channel.queueDeclare().queue

        // Generate a consumer tag
        String consumerTag = UUID.randomUUID().toString()

        // Create the sync object
        SynchronousQueue<MessageContext> replyHandoff = new SynchronousQueue<MessageContext>()

        // Define the handler
        DefaultConsumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String replyConsumerTag, Envelope replyEnvelope, AMQP.BasicProperties replyProperties, byte[] replyBody) throws IOException {
                MessageContext context = new MessageContext(
                    channel: null,
                    consumerTag: replyConsumerTag,
                    envelope: replyEnvelope,
                    properties: replyProperties,
                    body: replyBody
                )
                try {
                    replyHandoff.put(context)
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt()
                }
            }
        }

        // Start the consumer
        channel.basicConsume(properties.replyTo, false, consumerTag, true, true, null, consumer)

        // Send the message
        channel.basicPublish(exchange, routingKey, properties, body)

        // Wait for the reply
        MessageContext reply = (timeout < 0) ? replyHandoff.take() : replyHandoff.poll(timeout, TimeUnit.MILLISECONDS)

        // Close the channel
        channel.close()

        // If the reply is null, assume the timeout was reached
        if (reply == null) {
            throw new TimeoutException("timeout of ${timeout} milliseconds reached while waiting for a response in an RPC message to exchange '${exchange}' and routingKey '${routingKey}'")
        }

        // If auto convert is disabled, return the MessageContext
        if (!autoConvert) {
            return reply
        }

        return convertReply(reply.body)
    }

    /**
     * Sends a message to the bus and waits for a reply, up to the "timeout" property.
     *
     * @return
     * @throws TimeoutException
     * @throws ShutdownSignalException
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public Object rpc() throws TimeoutException, ShutdownSignalException, IOException, IllegalArgumentException {
        return doRpc()
    }

    /**
     * Sends a message to the bus and waits for a reply, up to the "timeout" property.
     *
     * This method returns a Message object if autoConvert is set to false, or some
     * other object type (string, list, map) if autoConvert is true.
     *
     * @param closure
     * @return
     * @throws TimeoutException
     * @throws ShutdownSignalException
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public Object rpc(Closure closure) throws TimeoutException, ShutdownSignalException, IOException, IllegalArgumentException {
        // Run the closure
        run closure

        // Send the message
        return doRpc()
    }

    /**
     * Sends a message to the bus and waits for a reply, up to the "timeout" property.
     *
     * This method returns a Message object if autoConvert is set to false, or some
     * other object type (string, list, map) if autoConvert is true.
     *
     * @param routingKey Routing key to send the message to.
     * @param body Message payload.
     * @return
     * @throws TimeoutException
     * @throws ShutdownSignalException
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public Object rpc(String routingKey, Object body) throws TimeoutException, ShutdownSignalException, IOException, IllegalArgumentException {
        // Set the params
        this.routingKey = routingKey
        this.body = body

        // Send the message
        return doRpc()
    }

    /**
     * Sends a message to the bus and waits for a reply, up to the "timeout" property.
     *
     * This method returns a Message object if autoConvert is set to false, or some
     * other object type (string, list, map) if autoConvert is true.
     *
     * @param exchange Exchange to send the message to.
     * @param routingKey Routing key to send the message to.
     * @param body Message payload.
     * @return
     * @throws TimeoutException
     * @throws ShutdownSignalException
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public Object rpc(String exchange, String routingKey, Object body) throws TimeoutException, ShutdownSignalException, IOException, IllegalArgumentException {
        // Set the params
        this.exchange = exchange
        this.routingKey = routingKey
        this.body = body

        // Send the message
        doRpc()
    }

    /**
     * Creates the message properties.
     *
     */
    protected AMQP.BasicProperties buildProperties() {
        // Create message properties
        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder()

        // Set any headers
        builder.headers(headers)

        // Content type
        if (contentType) {
            builder.contentType(contentType)
        }

        // Content encoding
        if (contentEncoding) {
            builder.contentEncoding(contentEncoding)
        }

        // Delivery mode
        if (deliveryMode in [1, 2]) {
            builder.deliveryMode(deliveryMode)
        }

        // Set priority
        if (priority) {
            builder.priority(priority)
        }

        // Set correlation id
        if (correlationId) {
            builder.correlationId(correlationId)
        }

        // Reply-to
        if (replyTo) {
            builder.replyTo(replyTo)
        }

        // Expiration
        if (expiration) {
            builder.expiration(expiration)
        }

        // Message ID
        if (messageId) {
            builder.messageId(messageId)
        }

        // Timestamp
        if (timestamp) {
            builder.timestamp(timestamp.getTime())
        }

        // Type
        if (type) {
            builder.type(type)
        }

        // User ID
        if (userId) {
            builder.userId(userId)
        }

        // Application ID
        if (appId) {
            builder.appId(appId)
        }

        return builder.build()
    }

    /**
     * Converts the payload object and creates the message object.
     *
     * @param source Object to convert.
     * @return Source object converted to a byte array.
     */
    protected byte[] convertMessage(Object source) {
        // If we were given a byte array, there's nothing to do
        if (source instanceof byte[]) {
            return source
        }

        // Check for lists or maps
        if (source instanceof List || source instanceof Map) {
            return new JsonBuilder(source).toString().getBytes()
        }

        // Check for domains (use the Grails JSON converter on purpose)
        if (grailsApplication.isDomainClass(source.getClass())) {
            return new JSON(source).toString().getBytes()
        }

        // Attempt automatic conversion (this won't always work)
        try {
            return source.toString().getBytes()
        }
        catch (Exception e) {
            throw new MessageConvertException(source.getClass())
        }
    }

    /**
     * Attempts to convert the message reply to an appropriate data type.
     *
     * @param body
     * @return
     */
    protected Object convertReply(byte[] body) {
        // Skip if auto convert is disabled
        if (!autoConvert) {
            return body
        }

        // Convert to a string
        String string
        try {
            string = new String(body)
        }
        catch (Exception e) {
            return body
        }

        // Attempt JSON conversion
        try {
            return new JsonSlurper().parseText(string)
        }
        catch (Exception e) {
            // continue
        }

        // Attempt integer
        if (string.isInteger()) {
            return string.toInteger()
        }

        return string
    }

    /**
     * Runs a passed closure to implement builder-style operation.
     *
     * @param closure
     */
    protected void run(Closure closure) {
        closure.delegate = this
        closure.resolveStrategy = Closure.OWNER_FIRST
        closure.run()
    }
}
