package com.budjb.rabbitmq.test

import static org.mockito.Mockito.*

import com.budjb.rabbitmq.RabbitMessageProperties

class RabbitMessagePublisherSendTests extends RabbitMessagePublisherTest {
    /**
     * Message to send with basic send tests.
     */
    private final String BASIC_PUBLISH_MESSAGE = 'foobar'

    /**
     * Test the send method given a routing key and body.
     */
    void testSendRoutingKey() {
        // Send a message
        rabbitMessagePublisher.send('test-queue', BASIC_PUBLISH_MESSAGE)

        // Verify the correct arguments were passed to the channel
        verify(channel).basicPublish(
            eq(''),
            eq('test-queue'),
            any(),
            eq(BASIC_PUBLISH_MESSAGE.getBytes())
        )
    }

    /**
     * Test the send method given an exchange, routing key, and body.
     */
    void testSendExchangeAndRoutingKey() {
        // Send a message
        rabbitMessagePublisher.send('test-exchange', 'test-routing-key', BASIC_PUBLISH_MESSAGE)

        // Verify the correct arguments were passed to the channel
        verify(channel).basicPublish(
            eq('test-exchange'),
            eq('test-routing-key'),
            any(),
            eq(BASIC_PUBLISH_MESSAGE.getBytes())
        )
    }

    /**
     * Test the send method given a properties object.
     */
    void testSendProperties() {
        // Send a message
        rabbitMessagePublisher.send(new RabbitMessageProperties().build {
            exchange = 'test-exchange'
            routingKey = 'test-routing-key'
            body = BASIC_PUBLISH_MESSAGE
        })

        // Verify the correct arguments were passed to the channel
        verify(channel).basicPublish(
            eq('test-exchange'),
            eq('test-routing-key'),
            any(),
            eq(BASIC_PUBLISH_MESSAGE.getBytes())
        )
    }

    /**
     * Test the send method given a closure.
     */
    void testSendClosure() {
        // Send a message
        rabbitMessagePublisher.send {
            exchange = 'test-exchange'
            routingKey = 'test-routing-key'
            body = BASIC_PUBLISH_MESSAGE
        }

        // Verify the correct arguments were passed to the channel
        verify(channel).basicPublish(
            eq('test-exchange'),
            eq('test-routing-key'),
            any(),
            eq(BASIC_PUBLISH_MESSAGE.getBytes())
        )
    }
}
