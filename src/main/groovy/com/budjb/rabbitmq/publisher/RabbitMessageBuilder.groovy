/*
 * Copyright 2013-2015 Bud Byrd
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
package com.budjb.rabbitmq.publisher

import com.rabbitmq.client.Channel
import com.rabbitmq.client.ShutdownSignalException
import grails.util.Holders

import java.util.concurrent.TimeoutException

@Deprecated
class RabbitMessageBuilder {
    /**
     * Default timeout for RPC calls (5 seconds).
     */
    static final int DEFAULT_TIMEOUT = 5000

    /**
     * Rabbit message publisher.
     */
    protected RabbitMessagePublisher rabbitMessagePublisher

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
     * Connection name.
     */
    String connection = null

    /**
     * Constructor
     *
     * Loads the rabbit template bean registered from the grails plugin.
     */
    RabbitMessageBuilder(Channel channel = null) {
        this.channel = channel
    }

    /**
     * Builds a RabbitMessageProperties object to submit to the message publisher.
     *
     * @return
     */
    RabbitMessageProperties buildMessageProperties() {
        RabbitMessageProperties properties = new RabbitMessageProperties()

        properties.appId = appId
        properties.autoConvert = autoConvert
        properties.body = body
        properties.channel = channel
        properties.connection = connection
        properties.contentEncoding = contentEncoding
        properties.contentType = contentType
        properties.correlationId = correlationId
        properties.deliveryMode = deliveryMode
        properties.exchange = exchange
        properties.setExpiration((String)expiration)
        properties.headers = headers
        properties.messageId = messageId
        properties.priority = priority
        properties.replyTo = replyTo
        properties.routingKey = routingKey
        properties.timeout = timeout
        properties.timestamp = timestamp
        properties.type = type
        properties.userId = userId

        return properties
    }

    /**
     * Returns the rabbit message publisher bean, loading it if necessary.
     *
     * @return
     */
    protected RabbitMessagePublisher getRabbitMessagePublisher() {
        if (!rabbitMessagePublisher) {
            rabbitMessagePublisher = Holders.applicationContext.getBean('rabbitMessagePublisher')
        }
        return rabbitMessagePublisher
    }

    /**
     * Sets the rabbit message publisher bean.
     *
     * @param rabbitMessagePublisher
     */
    void setRabbitMessagePublisher(RabbitMessagePublisher rabbitMessagePublisher) {
        this.rabbitMessagePublisher = rabbitMessagePublisher
    }

    /**
     * Sends a message to the rabbit service.
     *
     * @throws IllegalArgumentException
     */
    @Deprecated
    void send() throws IllegalArgumentException {
        getRabbitMessagePublisher().send(buildMessageProperties())
    }

    /**
     * Sends a message to the rabbit service.
     *
     * @param closure
     * @throws IllegalArgumentException
     */
    @Deprecated
    void send(Closure closure) throws IllegalArgumentException {
        // Run the closure
        run closure

        // Send the message
        getRabbitMessagePublisher().send(buildMessageProperties())
    }

    /**
     * Sends a message to the rabbit service.
     *
     * @param routingKey Routing key to send the message to.
     * @param body Message payload.
     * @throws IllegalArgumentException
     */
    @Deprecated
    void send(String routingKey, Object body) throws IllegalArgumentException {
        // Set the params
        this.routingKey = routingKey
        this.body = body

        // Send the message
        getRabbitMessagePublisher().send(buildMessageProperties())
    }

    /**
     * Sends a message to the rabbit service.
     *
     * @param exchange Exchange to send the message to.
     * @param routingKey Routing key to send the message to.
     * @param body Message payload.
     * @throws IllegalArgumentException
     */
    @Deprecated
    void send(String exchange, String routingKey, Object body) throws IllegalArgumentException {
        // Set the params
        this.exchange = exchange
        this.routingKey = routingKey
        this.body = body

        // Send the message
        getRabbitMessagePublisher().send(buildMessageProperties())
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
    @Deprecated
    Object rpc() throws TimeoutException, ShutdownSignalException, IOException, IllegalArgumentException {
        return getRabbitMessagePublisher().rpc(buildMessageProperties())
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
    @Deprecated
    Object rpc(Closure closure) throws TimeoutException, ShutdownSignalException, IOException, IllegalArgumentException {
        // Run the closure
        run closure

        // Send the message
        return getRabbitMessagePublisher().rpc(buildMessageProperties())
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
    @Deprecated
    Object rpc(String routingKey, Object body) throws TimeoutException, ShutdownSignalException, IOException, IllegalArgumentException {
        // Set the params
        this.routingKey = routingKey
        this.body = body

        // Send the message
        return getRabbitMessagePublisher().rpc(buildMessageProperties())
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
    @Deprecated
    Object rpc(String exchange, String routingKey, Object body) throws TimeoutException, ShutdownSignalException, IOException, IllegalArgumentException {
        // Set the params
        this.exchange = exchange
        this.routingKey = routingKey
        this.body = body

        // Send the message
        return getRabbitMessagePublisher().rpc(buildMessageProperties())
    }

    /**
     * Runs a passed closure to implement builder-style operation.
     *
     * @param closure
     */
    protected void run(Closure closure) {
        closure.delegate = this
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
    }
}
