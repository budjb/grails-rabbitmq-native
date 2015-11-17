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
package com.budjb.rabbitmq.publisher

import com.budjb.rabbitmq.connection.ConnectionManager
import com.budjb.rabbitmq.consumer.MessageContext
import com.budjb.rabbitmq.converter.MessageConverterManager
import com.budjb.rabbitmq.exception.MessageConvertException
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import com.rabbitmq.client.ShutdownSignalException
import org.apache.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired

import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class RabbitMessagePublisherImpl implements RabbitMessagePublisher {
    /**
     * Connection manager.
     */
    @Autowired
    ConnectionManager connectionManager

    /**
     * Message converter manager.
     */
    @Autowired
    MessageConverterManager messageConverterManager

    /**
     * Logger
     */
    Logger log = Logger.getLogger(RabbitMessagePublisherImpl)

    /**
     * Sends a Rabbit message with a given set of message properties.
     *
     * @param properties
     * @throws IllegalArgumentException
     */
    void send(RabbitMessageProperties properties) throws IllegalArgumentException {
        // Make sure an exchange or a routing key were provided
        if (!properties.exchange && !properties.routingKey) {
            throw new IllegalArgumentException("exchange and/or routing key required")
        }

        // Build properties
        BasicProperties basicProperties = properties.toBasicProperties()

        // Convert the object and create the message
        byte[] body = convertMessageToBytes(properties.body)

        // Whether the channel should be closed
        boolean closeChannel = false

        // If we weren't passed a channel, create a temporary one
        Channel channel = properties.channel
        if (!channel) {
            channel = connectionManager.createChannel(properties.connection)
            closeChannel = true
        }

        // Send the message
        channel.basicPublish(properties.exchange, properties.routingKey, basicProperties, body)

        // Close the channel
        if (closeChannel) {
            channel.close()
        }
    }

    /**
     * Builds a properties object with the given closure and sends a Rabbit message.
     *
     * @param closure Closure to configure the message properties.
     * @throws IllegalArgumentException
     */
    void send(Closure closure) throws IllegalArgumentException {
        send(createRabbitMessageProperties().build(closure))
    }

    /**
     * Sends a Rabbit message with a given routing key and payload.
     *
     * @param routingKey
     * @param body
     * @throws IllegalArgumentException
     */
    void send(String routingKey, Object body) throws IllegalArgumentException {
        send(createRabbitMessageProperties().build {
            delegate.routingKey = routingKey
            delegate.body = body
        })
    }

    /**
     * Sends a rabbit message with a given exchange, routing key, and payload.
     *
     * @param exchange
     * @param routingKey
     * @param body
     * @throws IllegalArgumentException
     */
    void send(String exchange, String routingKey, Object body) throws IllegalArgumentException {
        send(createRabbitMessageProperties().build {
            delegate.exchange = exchange
            delegate.routingKey = routingKey
            delegate.body = body
        })
    }

    /**
     * Attempts to convert an object to a byte array using any available message converters.
     *
     * @param Source object that needs conversion.
     * @return
     */
    protected byte[] convertMessageToBytes(Object source) {
        return messageConverterManager.convertToBytes(source)
    }

    /**
     * Attempts to convert the given byte array to another type via the message converters.
     *
     * @param input Byte array to convert.
     * @return An object converted from a byte array, or the byte array if no conversion could be done.
     */
    protected Object convertMessageFromBytes(byte[] input) {
        try {
            messageConverterManager.convertFromBytes(input)
        }
        catch (MessageConvertException ignore) {
            return input
        }
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
    Object rpc(RabbitMessageProperties properties) throws TimeoutException, ShutdownSignalException, IOException, IllegalArgumentException {
        // Make sure an exchange or a routing key were provided
        if (!properties.exchange && !properties.routingKey) {
            throw new IllegalArgumentException("exchange and/or routing key required")
        }

        // Convert the object and create the message
        byte[] body = convertMessageToBytes(properties.body)

        // Track whether channel should be closed
        boolean closeChannel = false

        // Track whether we've started consuming
        boolean consuming = false

        // Track the temporary queue name
        String temporaryQueue

        // If a channel wasn't given, create one
        Channel channel = properties.channel
        if (!channel) {
            channel = connectionManager.createChannel(properties.connection)
            closeChannel = true
        }

        // Generate a consumer tag
        String consumerTag = UUID.randomUUID().toString()

        try {
            // Create a temporary queue
            temporaryQueue = channel.queueDeclare().queue

            // Set the reply queue
            properties.replyTo = temporaryQueue

            // Build properties
            BasicProperties basicProperties = properties.toBasicProperties()

            // Create the sync object
            SynchronousQueue<MessageContext> replyHandoff = createResponseQueue()

            // Define the response consumer handler
            DefaultConsumer consumer = new DefaultConsumer(channel) {
                @Override
                void handleDelivery(String replyConsumerTag, Envelope replyEnvelope, BasicProperties replyProperties, byte[] replyBody) throws IOException {
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
                    catch (InterruptedException ignore) {
                        Thread.currentThread().interrupt()
                    }
                }
            }

            // Start the consumer and mark it
            channel.basicConsume(temporaryQueue, false, consumerTag, true, true, null, consumer)
            consuming = true

            // Send the message
            channel.basicPublish(properties.exchange, properties.routingKey, basicProperties, body)

            // Wait for the reply
            MessageContext reply = (properties.timeout < 0) ? replyHandoff.take() : replyHandoff.poll(properties.timeout, TimeUnit.MILLISECONDS)

            // If the reply is null, assume the timeout was reached
            if (reply == null) {
                throw new TimeoutException("timeout of ${properties.timeout} milliseconds reached while waiting for a response in an RPC message to exchange '${properties.exchange}' and routingKey '${properties.routingKey}'")
            }

            // If auto convert is disabled, return the MessageContext
            if (!properties.autoConvert) {
                return reply
            }

            return convertMessageFromBytes(reply.body)
        }
        finally {
            // If we've started consuming, stop consumption.
            // This cleans up some tracking objects internal to the RabbitMQ
            // library when using auto-recovering connections.
            // A memory leak results without this.
            if (consuming) {
                channel.basicCancel(consumerTag)
            }

            // Close the channel if a temporary one was opened
            if (closeChannel) {
                channel.close()
            }
        }
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
    Object rpc(Closure closure) throws TimeoutException, ShutdownSignalException, IOException, IllegalArgumentException {
        RabbitMessageProperties properties = createRabbitMessageProperties()
        properties.build(closure)
        return rpc(properties)
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
    Object rpc(String routingKey, Object body) throws TimeoutException, ShutdownSignalException, IOException, IllegalArgumentException {
        return rpc(createRabbitMessageProperties().build {
            delegate.routingKey = routingKey
            delegate.body = body
        })
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
    Object rpc(String exchange, String routingKey, Object body) throws TimeoutException, ShutdownSignalException, IOException, IllegalArgumentException {
        return rpc(createRabbitMessageProperties().build {
            delegate.exchange = exchange
            delegate.routingKey = routingKey
            delegate.body = body
        })
    }

    /**
     * Creates and returns a synchronous queue for use in the RPC consumer.
     *
     * @return
     */
    @Override
    SynchronousQueue<MessageContext> createResponseQueue() {
        return new SynchronousQueue<MessageContext>()
    }

    /**
     * Creates a new rabbit message properties instance.
     *
     * @return
     */
    RabbitMessageProperties createRabbitMessageProperties() {
        return new RabbitMessageProperties()
    }

    /**
     * Creates a new rabbit message publisher channel proxy.
     *
     * @return
     */
    RabbitMessagePublisherChannelProxy createRabbitMessagePublisherChannelProxy(Channel channel) {
        return new RabbitMessagePublisherChannelProxy(this, channel)
    }

    /**
     * Performs a series of operations with the same channel.
     *
     * @param Closure
     */
    @Override
    void withChannel(Closure closure) {
        Channel channel = connectionManager.createChannel()

        try {
            createRabbitMessagePublisherChannelProxy(channel).run(closure)
        }
        finally {
            if (channel.isOpen()) {
                channel.close()
            }
        }
    }

    /**
     * Performs a series of operations with the same channel created from the given connection.
     *
     * @param connection
     * @param closure
     */
    @Override
    void withChannel(String connection, Closure closure) {
        Channel channel = connectionManager.createChannel(connection)

        try {
            createRabbitMessagePublisherChannelProxy(channel).run(closure)
        }
        finally {
            if (channel.isOpen()) {
                channel.close()
            }
        }
    }

    /**
     * Performs a series of operations with the same channel and with confirms enabled.
     *
     * @param closure
     */
    @Override
    void withConfirms(Closure closure) {
        Channel channel = connectionManager.createChannel()

        try {
            channel.confirmSelect()

            createRabbitMessagePublisherChannelProxy(channel).run(closure)

            channel.waitForConfirms()
        }
        finally {
            if (channel.isOpen()) {
                channel.close()
            }
        }
    }

    /**
     * Performs a series of operations with the same channel created from the given connection
     * and with confirms enabled.
     *
     * @param connection
     * @param closure
     */
    @Override
    void withConfirms(String connection, Closure closure) {
        Channel channel = connectionManager.createChannel(connection)

        try {
            channel.confirmSelect()

            createRabbitMessagePublisherChannelProxy(channel).run(closure)

            channel.waitForConfirms()
        }
        finally {
            if (channel.isOpen()) {
                channel.close()
            }
        }
    }

    /**
     * Performs a series of operations with the same channel and with confirms enabled.
     *
     * @param timeout
     * @param closure
     */
    @Override
    void withConfirms(long timeout, Closure closure) {
        Channel channel = connectionManager.createChannel()

        try {
            channel.confirmSelect()

            createRabbitMessagePublisherChannelProxy(channel).run(closure)

            channel.waitForConfirms(timeout)
        }
        finally {
            if (channel.isOpen()) {
                channel.close()
            }
        }
    }

    /**
     * Performs a series of operations with the same channel created from the given connection
     * and with confirms enabled.
     *
     * @param connection
     * @param timeout
     * @param closure
     */
    @Override
    void withConfirms(String connection, long timeout, Closure closure) {
        Channel channel = connectionManager.createChannel(connection)

        try {
            channel.confirmSelect()

            createRabbitMessagePublisherChannelProxy(channel).run(closure)

            channel.waitForConfirms(timeout)
        }
        finally {
            if (channel.isOpen()) {
                channel.close()
            }
        }
    }

    /**
     * Performs a series of operations with the same channel and with confirms enabled.
     * This method will throw an exception if any messages are nack'd.
     *
     * @param closure
     */
    @Override
    void withConfirmsOrDie(Closure closure) {
        Channel channel = connectionManager.createChannel()

        try {
            channel.confirmSelect()

            createRabbitMessagePublisherChannelProxy(channel).run(closure)

            channel.waitForConfirmsOrDie()
        }
        finally {
            if (channel.isOpen()) {
                channel.close()
            }
        }
    }

    /**
     * Performs a series of operations with the same channel created from the given connection
     * and with confirms enabled. This method will throw an exception if any messaged are nack'd.
     *
     * @param connection
     * @param closure
     */
    @Override
    void withConfirmsOrDie(String connection, Closure closure) {
        Channel channel = connectionManager.createChannel(connection)

        try {
            channel.confirmSelect()

            createRabbitMessagePublisherChannelProxy(channel).run(closure)

            channel.waitForConfirmsOrDie()
        }
        finally {
            if (channel.isOpen()) {
                channel.close()
            }
        }
    }

    /**
     * Performs a series of operations with the same channel and with confirms enabled.
     * This method will throw an exception if any messaged are nack'd.
     *
     * @param timeout
     * @param closure
     */
    @Override
    void withConfirmsOrDie(long timeout, Closure closure) {
        Channel channel = connectionManager.createChannel()

        try {
            channel.confirmSelect()

            createRabbitMessagePublisherChannelProxy(channel).run(closure)

            channel.waitForConfirmsOrDie(timeout)
        }
        finally {
            if (channel.isOpen()) {
                channel.close()
            }
        }
    }

    /**
     * Performs a series of operations with the same channel created from the given connection
     * and with confirms enabled. This method will throw an exception if any messaged are nack'd.
     *
     * @param connection
     * @param timeout
     * @param closure
     */
    @Override
    void withConfirmsOrDie(String connection, long timeout, Closure closure) {
        Channel channel = connectionManager.createChannel(connection)

        try {
            channel.confirmSelect()

            createRabbitMessagePublisherChannelProxy(channel).run(closure)

            channel.waitForConfirmsOrDie(timeout)
        }
        finally {
            if (channel.isOpen()) {
                channel.close()
            }
        }
    }
}
