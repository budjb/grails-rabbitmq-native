package com.budjb.rabbitmq.test

import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeoutException

import spock.lang.Specification

import com.budjb.rabbitmq.*
import com.budjb.rabbitmq.converter.*
import com.rabbitmq.client.Channel
import com.rabbitmq.client.impl.AMQImpl.Queue.DeclareOk

import grails.test.mixin.*

class RabbitMessagePublisherRpcSpec extends Specification {
    /**
     * Message to publish in the basic RPC tests.
     */
    private static final String BASIC_PUBLISH_MESSAGE = 'Knock knock...'

    /**
     * Message to respond with in the basic RPC test.
     */
    private static final String BASIC_RESPONSE_MESSAGE = 'Who\'s there?'

    /**
     * Exchange to send with basic send tests.
     */
    private static final String BASIC_PUBLISH_EXCHANGE = 'test-exchange'

    /**
     * Routing key to send with basic send tests.
     */
    private static final String BASIC_PUBLISH_ROUTING_KEY = 'test-routing-key'

    /**
     * Mocked rabbit context.
     */
    RabbitContext rabbitContext

    /**
     * Live message publisher instance.
     */
    RabbitMessagePublisher rabbitMessagePublisher

    /**
     * Message converter manager.
     */
    MessageConverterManager messageConverterManager

    /**
     * Mocked channel.
     */
    Channel channel

    def setup() {
        // Mock the rabbit context
        rabbitContext = Mock(RabbitContext)

        // Create the message converter manager
        messageConverterManager = new MessageConverterManager()
        messageConverterManager.registerMessageConverter(new IntegerMessageConverter())
        messageConverterManager.registerMessageConverter(new MapMessageConverter())
        messageConverterManager.registerMessageConverter(new ListMessageConverter())
        messageConverterManager.registerMessageConverter(new GStringMessageConverter())
        messageConverterManager.registerMessageConverter(new StringMessageConverter())

        // Inject the message converter manager into the rabbit context
        rabbitContext.getMessageConverters() >> messageConverterManager.getMessageConverters()

        // Mock a channel
        channel = Mock(Channel)

        // Mock a channel return from the rabbit context
        rabbitContext.createChannel(null) >> channel

        // Create the message publisher
        rabbitMessagePublisher = new RabbitMessagePublisher()
        rabbitMessagePublisher.rabbitContext = rabbitContext
    }

    def mockBasicRpc(byte[] response) {
        // Mock temporary queue creation
        channel.queueDeclare() >> new DeclareOk('temporary-queue', 0, 0)

        // Mock a sync queue for the rpc consumer
        SynchronousQueue<MessageContext> queue = Mock(SynchronousQueue)

        // Set up the publisher as a spy (we need partial mocking for rpc calls)
        rabbitMessagePublisher = Spy(RabbitMessagePublisher)
        rabbitMessagePublisher.createResponseQueue() >> queue

        rabbitMessagePublisher.rabbitContext = rabbitContext

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
    /**
     * Tests the RPC method given a routing key and body.
     */
    def 'RPC with only a routing key'() {
        setup:
        mockBasicRpc(BASIC_RESPONSE_MESSAGE.getBytes())

        when:
        String response = rabbitMessagePublisher.rpc(BASIC_PUBLISH_ROUTING_KEY, BASIC_PUBLISH_MESSAGE)

        then:
        response == BASIC_RESPONSE_MESSAGE
        1 * channel.basicPublish('', BASIC_PUBLISH_ROUTING_KEY, _, BASIC_PUBLISH_MESSAGE.getBytes())
    }

    /**
     * Tests the RPC method given an exchange, routing key, and body.
     */
    def 'RPC with an exchange and routing key'() {
        setup:
        mockBasicRpc(BASIC_RESPONSE_MESSAGE.getBytes())

        when:
        String response = rabbitMessagePublisher.rpc(BASIC_PUBLISH_EXCHANGE, BASIC_PUBLISH_ROUTING_KEY, BASIC_PUBLISH_MESSAGE)

        then:
        response == BASIC_RESPONSE_MESSAGE
        1 * channel.basicPublish(BASIC_PUBLISH_EXCHANGE, BASIC_PUBLISH_ROUTING_KEY, _, BASIC_PUBLISH_MESSAGE.getBytes())
    }

    /**
     * Tests the RPC method given a properties object.
     */
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

    /**
     * Tests the RPC method given a closure.
     */
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

    /**
     * Test the timeout functionality for an RPC call.
     */
    def 'Ensure that an RPC timeout throws an exception'() {
        setup:
        channel.queueDeclare() >> new DeclareOk('temporary-queue', 0, 0)
        SynchronousQueue<MessageContext> queue = new SynchronousQueue<MessageContext>()
        rabbitMessagePublisher = Spy(RabbitMessagePublisher)
        rabbitMessagePublisher.createResponseQueue() >> queue
        rabbitMessagePublisher.rabbitContext = rabbitContext

        when:
        rabbitMessagePublisher.rpc {
            routingKey = BASIC_PUBLISH_ROUTING_KEY
            timeout = 500
        }

        then:
        thrown TimeoutException
    }

    /**
     * Test that no channel is created from the rabbit context if one is provided.
     */
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
        0 * rabbitContext.createChannel(_)
        0 * channel.close()
    }

    /**
     * Test that a channel is created and closed when one is not provided.
     */
    def 'If no channel is provided, ensure one is created and closed'() {
        setup:
        mockBasicRpc(BASIC_RESPONSE_MESSAGE.getBytes())

        when:
        rabbitMessagePublisher.rpc {
            routingKey = BASIC_PUBLISH_ROUTING_KEY
        }

        then:
        1 * rabbitContext.createChannel(null) >> channel
        1 * channel.close()
    }

    /**
     * Verify mechanical operation of publishing and consuming.
     */
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
}
