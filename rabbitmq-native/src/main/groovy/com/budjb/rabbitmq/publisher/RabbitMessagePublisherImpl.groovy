/*
 * Copyright 2017 Bud Byrd
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
import com.budjb.rabbitmq.converter.ByteToObjectInput
import com.budjb.rabbitmq.converter.MessageConverterManager
import com.budjb.rabbitmq.converter.ObjectToByteInput
import com.budjb.rabbitmq.converter.ObjectToByteResult
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import com.rabbitmq.client.ShutdownSignalException
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Slf4j
@CompileStatic
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
     * Sends a Rabbit message with a given set of message properties.
     *
     * @param properties
     * @throws IllegalArgumentException
     */
    void send(RabbitMessageProperties properties) throws IllegalArgumentException {
        if (!properties.exchange && !properties.routingKey) {
            throw new IllegalArgumentException("exchange and/or routing key required")
        }

        byte[] body = convert(properties)

        BasicProperties basicProperties = properties.toBasicProperties()

        boolean closeChannel = false

        Channel channel = properties.channel
        if (!channel) {
            channel = connectionManager.createChannel(properties.connection)
            closeChannel = true
        }

        channel.basicPublish(properties.exchange, properties.routingKey, basicProperties, body)

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
    void send(@DelegatesTo(RabbitMessageProperties) Closure closure) throws IllegalArgumentException {
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
        if (!properties.exchange && !properties.routingKey) {
            throw new IllegalArgumentException("exchange and/or routing key required")
        }

        byte[] body = convert(properties)

        boolean closeChannel = false

        boolean consuming = false

        String temporaryQueue

        Channel channel = properties.channel
        if (!channel) {
            channel = connectionManager.createChannel(properties.connection)
            closeChannel = true
        }

        String consumerTag = UUID.randomUUID().toString()

        try {
            temporaryQueue = channel.queueDeclare().queue

            properties.replyTo = temporaryQueue

            BasicProperties basicProperties = properties.toBasicProperties()

            SynchronousQueue<MessageContext> replyHandOff = createResponseQueue()

            DefaultConsumer consumer = new DefaultConsumer(channel) {
                @Override
                void handleDelivery(String replyConsumerTag, Envelope replyEnvelope, BasicProperties replyProperties, byte[] replyBody)
                    throws IOException {
                    MessageContext context = new MessageContext(
                        channel: (Channel) null,
                        consumerTag: replyConsumerTag,
                        envelope: replyEnvelope,
                        properties: replyProperties,
                        body: replyBody
                    )
                    try {
                        replyHandOff.put(context)
                    }
                    catch (InterruptedException ignore) {
                        Thread.currentThread().interrupt()
                    }
                }
            }

            channel.basicConsume(temporaryQueue, false, consumerTag, true, true, null, consumer)
            consuming = true

            channel.basicPublish(properties.exchange, properties.routingKey, basicProperties, body)

            MessageContext reply = (properties.timeout < 0) ? replyHandOff.take() : replyHandOff.poll(properties.timeout, TimeUnit.MILLISECONDS)

            if (reply == null) {
                throw new TimeoutException(
                    "timeout of ${properties.timeout} milliseconds reached while waiting for a response in an RPC message to exchange " +
                        "'${properties.exchange}' and routingKey '${properties.routingKey}'")
            }

            if (!properties.autoConvert) {
                return reply
            }

            return convert(reply)
        }
        finally {
            // If we've started consuming, stop consumption.
            // This cleans up some tracking objects internal to the RabbitMQ
            // library when using auto-recovering connections.
            // A memory leak results without this.
            if (consuming) {
                channel.basicCancel(consumerTag)
            }

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
    Object rpc(@DelegatesTo(RabbitMessageProperties) Closure closure)
        throws TimeoutException, ShutdownSignalException, IOException, IllegalArgumentException {
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
    Object rpc(String exchange, String routingKey, Object body)
        throws TimeoutException, ShutdownSignalException, IOException, IllegalArgumentException {
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
    void withChannel(@DelegatesTo(RabbitMessagePublisher) Closure closure) {
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
    void withChannel(String connection, @DelegatesTo(RabbitMessagePublisher) Closure closure) {
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
    void withConfirms(@DelegatesTo(RabbitMessagePublisher) Closure closure) {
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
    void withConfirms(String connection, @DelegatesTo(RabbitMessagePublisher) Closure closure) {
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
    void withConfirms(long timeout, @DelegatesTo(RabbitMessagePublisher) Closure closure) {
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
    void withConfirms(String connection, long timeout, @DelegatesTo(RabbitMessagePublisher) Closure closure) {
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
    void withConfirmsOrDie(@DelegatesTo(RabbitMessagePublisher) Closure closure) {
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
    void withConfirmsOrDie(String connection, @DelegatesTo(RabbitMessagePublisher) Closure closure) {
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
    void withConfirmsOrDie(long timeout, @DelegatesTo(RabbitMessagePublisher) Closure closure) {
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
    void withConfirmsOrDie(String connection, long timeout, @DelegatesTo(RabbitMessagePublisher) Closure closure) {
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

    /**
     * Converts the body contained in the message properties. This will set the content type of
     * the message if one has not already been set.
     *
     * @param properties
     * @return
     */
    protected byte[] convert(RabbitMessageProperties properties) {
        if (properties.getBody() instanceof byte[]) {
            return (byte[]) properties.getBody()
        }

        ObjectToByteResult result = messageConverterManager.convert(new ObjectToByteInput(properties.getBody(), properties.getContentType()))

        if (!result) {
            return null
        }

        if (!properties.getContentType()) {
            properties.setContentType(result.getMimeType().toString())
        }

        return result.getResult()
    }

    /**
     * Converts the body contained in the message context. Will attempt to respect the content type
     * and/or the character set of the message if possible.
     *
     * @param messageContext
     * @return
     */
    protected Object convert(MessageContext messageContext) {
        return messageConverterManager.convert(new ByteToObjectInput(messageContext.getBody(), messageContext.getProperties().getContentType()))?.getResult()
    }
}
