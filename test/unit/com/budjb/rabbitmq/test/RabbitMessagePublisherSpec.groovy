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
package com.budjb.rabbitmq.test

import com.budjb.rabbitmq.connection.ConnectionManager
import com.budjb.rabbitmq.consumer.MessageContext
import com.budjb.rabbitmq.converter.*
import com.budjb.rabbitmq.exception.MessageConvertException
import com.budjb.rabbitmq.publisher.RabbitMessageProperties
import com.budjb.rabbitmq.publisher.RabbitMessagePublisherImpl
import com.rabbitmq.client.Channel
import com.rabbitmq.client.impl.AMQImpl.Queue.DeclareOk
import spock.lang.Specification

import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeoutException

class RabbitMessagePublisherSpec extends Specification {
    private static final String BASIC_PUBLISH_MESSAGE = 'Knock knock...'
    private static final String BASIC_RESPONSE_MESSAGE = 'Who\'s there?'
    private static final String BASIC_PUBLISH_EXCHANGE = 'test-exchange'
    private static final String BASIC_PUBLISH_ROUTING_KEY = 'test-routing-key'

    ConnectionManager connectionManager
    RabbitMessagePublisherImpl rabbitMessagePublisher
    MessageConverterManager messageConverterManager
    Channel channel

    def mockBasicRpc(byte[] response) {
        // Mock temporary queue creation
        channel.queueDeclare() >> new DeclareOk('temporary-queue', 0, 0)

        // Mock a sync queue for the rpc consumer
        SynchronousQueue<MessageContext> queue = Mock(SynchronousQueue)

        // Set up the publisher as a spy (we need partial mocking for rpc calls)
        rabbitMessagePublisher = Spy(RabbitMessagePublisherImpl)
        rabbitMessagePublisher.createResponseQueue() >> queue
        rabbitMessagePublisher.connectionManager = connectionManager
        rabbitMessagePublisher.messageConverterManager = messageConverterManager

        // Create a mocked response message context
        MessageContext responseMessageContext = new MessageContext(
            channel: null,
            consumerTag: null,
            envelope: null,
            properties: null,
            body: response
        )

        // Mock the poll
        queue.poll(*_) >> responseMessageContext
        queue.take() >> responseMessageContext

    }

    def setup() {
        channel = Mock(Channel)

        connectionManager = Mock(ConnectionManager)
        connectionManager.createChannel(null) >> channel

        messageConverterManager = new MessageConverterManagerImpl()
        messageConverterManager.register(new IntegerMessageConverter())
        messageConverterManager.register(new MapMessageConverter())
        messageConverterManager.register(new ListMessageConverter())
        messageConverterManager.register(new GStringMessageConverter())
        messageConverterManager.register(new StringMessageConverter())

        rabbitMessagePublisher = new RabbitMessagePublisherImpl()
        rabbitMessagePublisher.connectionManager = connectionManager
        rabbitMessagePublisher.messageConverterManager = messageConverterManager
    }

    def 'Ensure setMessageConverterManager(MessageConverterManager) sets the property correctly'() {
        setup:
        MessageConverterManager messageConverterManager = Mock(MessageConverterManager)

        when:
        rabbitMessagePublisher.setMessageConverterManager(messageConverterManager)

        then:
        rabbitMessagePublisher.messageConverterManager == messageConverterManager
    }

    def 'Ensure setConnectionManager(ConnectionManager) sets the property correctly'() {
        setup:
        ConnectionManager connectionManager = Mock(ConnectionManager)

        when:
        rabbitMessagePublisher.setConnectionManager(connectionManager)

        then:
        rabbitMessagePublisher.connectionManager == connectionManager
    }

    def 'Basic send() with only a routing key'() {
        when:
        rabbitMessagePublisher.send(BASIC_PUBLISH_ROUTING_KEY, BASIC_PUBLISH_MESSAGE)

        then:
        1 * channel.basicPublish('', BASIC_PUBLISH_ROUTING_KEY, _, BASIC_PUBLISH_MESSAGE.getBytes())
    }

    def 'Basic send() with an exchange and routing key'() {
        when:
        rabbitMessagePublisher.send(BASIC_PUBLISH_EXCHANGE, BASIC_PUBLISH_ROUTING_KEY, BASIC_PUBLISH_MESSAGE)

        then:
        1 * channel.basicPublish(BASIC_PUBLISH_EXCHANGE, BASIC_PUBLISH_ROUTING_KEY, _, BASIC_PUBLISH_MESSAGE.getBytes())
    }

    def 'Basic send() with a provided RabbitMessageProperties object'() {
        when:
        rabbitMessagePublisher.send(new RabbitMessageProperties().build {
            exchange = BASIC_PUBLISH_EXCHANGE
            routingKey = BASIC_PUBLISH_ROUTING_KEY
            body = BASIC_PUBLISH_MESSAGE
        })

        then:
        1 * channel.basicPublish(BASIC_PUBLISH_EXCHANGE, BASIC_PUBLISH_ROUTING_KEY, _, BASIC_PUBLISH_MESSAGE.getBytes())
    }

    def 'Basic send() configured by a closure'() {
        when:
        rabbitMessagePublisher.send {
            exchange = BASIC_PUBLISH_EXCHANGE
            routingKey = BASIC_PUBLISH_ROUTING_KEY
            body = BASIC_PUBLISH_MESSAGE
        }

        then:
        1 * channel.basicPublish(BASIC_PUBLISH_EXCHANGE, BASIC_PUBLISH_ROUTING_KEY, _, BASIC_PUBLISH_MESSAGE.getBytes())
    }

    def 'Send with no parameters provided (routing key and/or exchange are required)'() {
        when:
        rabbitMessagePublisher.send {}

        then:
        thrown IllegalArgumentException
    }

    def 'If a channel is provided ensure one\'s not created and it\'s not closed'() {
        setup:
        Channel channel = Mock(Channel)

        when:
        rabbitMessagePublisher.send {
            routingKey = BASIC_PUBLISH_ROUTING_KEY
            delegate.channel = channel
        }

        then:
        0 * connectionManager.createChannel()
        0 * channel.close()
    }

    def 'If no channel is provided, ensure one\'s created and closed'() {
        when:
        rabbitMessagePublisher.send {
            routingKey = BASIC_PUBLISH_ROUTING_KEY
            body = 'asdf'
        }

        then:
        1 * connectionManager.createChannel(null) >> channel
        1 * channel.close()
    }

    def 'Ensure an exception is thrown when content can\'t be marshaled'() {
        when:
        rabbitMessagePublisher.send(BASIC_PUBLISH_ROUTING_KEY, new DummyObject())

        then:
        thrown MessageConvertException
    }

    def 'RPC with only a routing key'() {
        setup:
        mockBasicRpc(BASIC_RESPONSE_MESSAGE.getBytes())

        when:
        String response = rabbitMessagePublisher.rpc(BASIC_PUBLISH_ROUTING_KEY, BASIC_PUBLISH_MESSAGE)

        then:
        response == BASIC_RESPONSE_MESSAGE
        1 * channel.basicPublish('', BASIC_PUBLISH_ROUTING_KEY, _, BASIC_PUBLISH_MESSAGE.getBytes())
    }

    def 'RPC with an exchange and routing key'() {
        setup:
        mockBasicRpc(BASIC_RESPONSE_MESSAGE.getBytes())

        when:
        String response = rabbitMessagePublisher.rpc(BASIC_PUBLISH_EXCHANGE, BASIC_PUBLISH_ROUTING_KEY, BASIC_PUBLISH_MESSAGE)

        then:
        response == BASIC_RESPONSE_MESSAGE
        1 * channel.basicPublish(BASIC_PUBLISH_EXCHANGE, BASIC_PUBLISH_ROUTING_KEY, _, BASIC_PUBLISH_MESSAGE.getBytes())
    }

    def 'RPC call with a RabbitMessageProperties object'() {
        setup:
        mockBasicRpc(BASIC_RESPONSE_MESSAGE.getBytes())

        when:
        String response = rabbitMessagePublisher.rpc(new RabbitMessageProperties().build {
            exchange = BASIC_PUBLISH_EXCHANGE
            routingKey = BASIC_PUBLISH_ROUTING_KEY
            body = BASIC_PUBLISH_MESSAGE
        })

        then:
        response == BASIC_RESPONSE_MESSAGE
        1 * channel.basicPublish(BASIC_PUBLISH_EXCHANGE, BASIC_PUBLISH_ROUTING_KEY, _, BASIC_PUBLISH_MESSAGE.getBytes())
    }

    def 'RPC call configured by a closure'() {
        setup:
        mockBasicRpc(BASIC_RESPONSE_MESSAGE.getBytes())

        when:
        String response = rabbitMessagePublisher.rpc {
            exchange = BASIC_PUBLISH_EXCHANGE
            routingKey = BASIC_PUBLISH_ROUTING_KEY
            body = BASIC_PUBLISH_MESSAGE
        }

        then:
        response == BASIC_RESPONSE_MESSAGE
        1 * channel.basicPublish(BASIC_PUBLISH_EXCHANGE, BASIC_PUBLISH_ROUTING_KEY, _, BASIC_PUBLISH_MESSAGE.getBytes())
    }

    def 'Ensure that an RPC timeout throws an exception'() {
        setup:
        channel.queueDeclare() >> new DeclareOk('temporary-queue', 0, 0)
        SynchronousQueue<MessageContext> queue = new SynchronousQueue<MessageContext>()
        rabbitMessagePublisher = Spy(RabbitMessagePublisherImpl)
        rabbitMessagePublisher.createResponseQueue() >> queue
        rabbitMessagePublisher.connectionManager = connectionManager
        rabbitMessagePublisher.messageConverterManager = messageConverterManager

        when:
        rabbitMessagePublisher.rpc {
            routingKey = BASIC_PUBLISH_ROUTING_KEY
            timeout = 500
        }

        then:
        thrown TimeoutException
    }

    def 'If a channel is provided, ensure it is not closed and another one is not created'() {
        setup:
        mockBasicRpc(BASIC_RESPONSE_MESSAGE.getBytes())
        Channel channel = Mock(Channel)
        channel.queueDeclare() >> new DeclareOk('temporary-queue', 0, 0)

        when:
        rabbitMessagePublisher.rpc {
            routingKey = BASIC_PUBLISH_ROUTING_KEY
            delegate.channel = channel
        }

        then:
        0 * connectionManager.createChannel(_)
        0 * channel.close()
    }

    def 'If no channel is provided, ensure one is created and closed'() {
        setup:
        mockBasicRpc(BASIC_RESPONSE_MESSAGE.getBytes())

        when:
        rabbitMessagePublisher.rpc {
            routingKey = BASIC_PUBLISH_ROUTING_KEY
        }

        then:
        1 * connectionManager.createChannel(null) >> channel
        1 * channel.close()
    }

    def 'Verify that an RPC call publishes a message, consumes from a queue, and cancels consuming'() {
        setup:
        mockBasicRpc(BASIC_RESPONSE_MESSAGE.getBytes())

        when:
        rabbitMessagePublisher.rpc {
            routingKey = BASIC_PUBLISH_ROUTING_KEY
        }

        then:
        1 * channel.basicPublish(*_)
        1 * channel.basicConsume(*_)
        1 * channel.basicCancel(*_)
    }

    /**
     * A dummy class used to test invalid message conversion.
     */
    class DummyObject {

    }
}
