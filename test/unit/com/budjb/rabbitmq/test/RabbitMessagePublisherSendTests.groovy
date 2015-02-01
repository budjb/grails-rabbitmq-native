package com.budjb.rabbitmq.test

import static org.mockito.Mockito.*

import com.budjb.rabbitmq.RabbitMessageProperties
import com.rabbitmq.client.Channel

class RabbitMessagePublisherSendTests extends RabbitMessagePublisherTest {
    /**
     * A dummy class used to test invalid message conversion.
     */
    class DummyObject {

    }

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
     * Test the send method given a routing key and body.
     */
    void testSendRoutingKey() {
        // Send a message
        rabbitMessagePublisher.send(BASIC_PUBLISH_ROUTING_KEY, BASIC_PUBLISH_MESSAGE)

        // Verify the correct arguments were passed to the channel
        verify(channel).basicPublish(
            eq(''),
            eq(BASIC_PUBLISH_ROUTING_KEY),
            any(),
            eq(BASIC_PUBLISH_MESSAGE.getBytes())
        )
    }

    /**
     * Test the send method given an exchange, routing key, and body.
     */
    void testSendExchangeAndRoutingKey() {
        // Send a message
        rabbitMessagePublisher.send(BASIC_PUBLISH_EXCHANGE, BASIC_PUBLISH_ROUTING_KEY, BASIC_PUBLISH_MESSAGE)

        // Verify the correct arguments were passed to the channel
        verify(channel).basicPublish(
            eq(BASIC_PUBLISH_EXCHANGE),
            eq(BASIC_PUBLISH_ROUTING_KEY),
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
            exchange = BASIC_PUBLISH_EXCHANGE
            routingKey = BASIC_PUBLISH_ROUTING_KEY
            body = BASIC_PUBLISH_MESSAGE
        })

        // Verify the correct arguments were passed to the channel
        verify(channel).basicPublish(
            eq(BASIC_PUBLISH_EXCHANGE),
            eq(BASIC_PUBLISH_ROUTING_KEY),
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
            exchange = BASIC_PUBLISH_EXCHANGE
            routingKey = BASIC_PUBLISH_ROUTING_KEY
            body = BASIC_PUBLISH_MESSAGE
        }

        // Verify the correct arguments were passed to the channel
        verify(channel).basicPublish(
            eq(BASIC_PUBLISH_EXCHANGE),
            eq(BASIC_PUBLISH_ROUTING_KEY),
            any(),
            eq(BASIC_PUBLISH_MESSAGE.getBytes())
        )
    }

    /**
     * Test for failure when no exchange and routing key are present.
     */
    void testMissingExchangeAndRoutingKey() {
        shouldFail(IllegalArgumentException) {
            rabbitMessagePublisher.send {
            }
        }
    }

    /**
     * Test that no channel is created from the rabbit context if one is provided.
     */
    void testProvidedChannel() {
        // Mock a new channel
        Channel channel = mock(Channel)

        // Send a message
        rabbitMessagePublisher.send {
            routingKey = BASIC_PUBLISH_ROUTING_KEY
            delegate.channel = channel
        }

        // Ensure a channel was not created from the rabbit context
        verify(rabbitContext, never()).createChannel()

        // Ensure the channel was not closed
        verify(channel, never()).close()
    }

    /**
     * Test that a channel is created and closed when one is not provided.
     */
    void testCreatedChannel() {
        // Send a message
        rabbitMessagePublisher.send {
            routingKey = BASIC_PUBLISH_ROUTING_KEY
        }

        // Ensure a channel was created
        verify(rabbitContext, times(1)).createChannel(null)

        // Ensure the channel was closed
        verify(channel, times(1)).close()
    }

    /**
     * Tests that a failure occurs when content marshaling fails.
     */
    void testInvalidConversion() {
        shouldFail(IllegalArgumentException) {
            rabbitMessagePublisher.send(BASIC_PUBLISH_ROUTING_KEY, new DummyObject())
        }
    }
}
