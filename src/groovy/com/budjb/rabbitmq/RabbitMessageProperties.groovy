package com.budjb.rabbitmq

import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.Channel

import groovy.lang.Closure

class RabbitMessageProperties {
    /**
     * Default timeout for RPC calls (5 seconds).
     */
    public static final int DEFAULT_TIMEOUT = 5000

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
     * Connection name.
     */
    String connection = null

    /**
     * Channel to publish messages through.
     */
    Channel channel

    /**
     * Builds the properties class from a provided closure and returns itself.
     *
     * @param closure
     * @return This object.
     */
    public RabbitMessageProperties build(Closure closure) {
        run(closure)
        return this
    }

    /**
     * Runs the provided closure.
     *
     * @param closure
     */
    protected void run(Closure closure) {
        closure.delegate = this
        closure.resolveStrategy = Closure.OWNER_FIRST
        closure.run()
    }

    /**
     * Creates an AMQP basic properties object suitable for use in publishing messages.
     *
     * @return
     */
    public BasicProperties toBasicProperties() {
        // Create message properties
        BasicProperties.Builder builder = new BasicProperties.Builder()

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
}
