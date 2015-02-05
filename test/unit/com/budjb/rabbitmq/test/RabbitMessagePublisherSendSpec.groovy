package com.budjb.rabbitmq.test

import spock.lang.Specification

import com.budjb.rabbitmq.*
import com.budjb.rabbitmq.converter.*
import com.rabbitmq.client.Channel

import grails.test.mixin.*

class RabbitMessagePublisherSendSpec extends Specification {
    /**
     * Message to send with basic send tests.
     */
    private static final String BASIC_PUBLISH_MESSAGE = 'foobar'

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

    /**
     * Sets up the basic requirements for a mocked RabbitMQ system.
     */
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

    /**
     * A dummy class used to test invalid message conversion.
     */
    class DummyObject {

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
        rabbitMessagePublisher.send { }

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
        0 * rabbitContext.createChannel()
        0 * channel.close()
    }

    def 'If no channel is provided, ensure one\'s created and closed'() {
        when:
        rabbitMessagePublisher.send {
            routingKey = BASIC_PUBLISH_ROUTING_KEY
            body = 'asdf'
        }

        then:
        1 * rabbitContext.createChannel(null) >> channel
        1 * channel.close()
    }

    def 'Ensure an exception is thrown when content can\'t be marshaled'() {
        when:
        rabbitMessagePublisher.send(BASIC_PUBLISH_ROUTING_KEY, new DummyObject())

        then:
        thrown IllegalArgumentException
    }
}
