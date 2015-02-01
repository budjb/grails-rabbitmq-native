package com.budjb.rabbitmq.test

import static org.mockito.Mockito.*

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeoutException;

import com.budjb.rabbitmq.MessageContext;
import com.budjb.rabbitmq.RabbitMessageProperties
import com.rabbitmq.client.impl.AMQImpl.Queue.DeclareOk;

class RabbitMessagePublisherRpcTests extends RabbitMessagePublisherTest {
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
     * Tests the RPC method given a routing key and body.
     */
    void testRpcRoutingKey() {
        // Mock for a basic RPC test
        mockBasicRpc(BASIC_RESPONSE_MESSAGE.getBytes())

        // Make the RPC call
        String response = rabbitMessagePublisher.rpc(BASIC_PUBLISH_ROUTING_KEY, BASIC_PUBLISH_MESSAGE)

        // Ensure response conversion worked
        assert response == BASIC_RESPONSE_MESSAGE : "Expected response \"$BASIC_RESPONSE_MESSAGE\"; received \"$response\""

        // Ensure the correct bits were sent to the publish call
        verify(channel).basicPublish(
            eq(''),
            eq(BASIC_PUBLISH_ROUTING_KEY),
            any(),
            eq(BASIC_PUBLISH_MESSAGE.getBytes())
        )
    }

    /**
     * Tests the RPC method given an exchange, routing key, and body.
     */
    void testRpcExchangeAndRoutingKey() {
        // Mock for a basic RPC test
        mockBasicRpc(BASIC_RESPONSE_MESSAGE.getBytes())

        // Make the RPC call
        String response = rabbitMessagePublisher.rpc(BASIC_PUBLISH_EXCHANGE, BASIC_PUBLISH_ROUTING_KEY, BASIC_PUBLISH_MESSAGE)

        // Ensure the correct bits were sent to the publish call
        assert response == BASIC_RESPONSE_MESSAGE : "Expected response \"$BASIC_RESPONSE_MESSAGE\"; received \"$response\""

        // Ensure the correct bits were sent to the publish call
        verify(channel).basicPublish(
            eq(BASIC_PUBLISH_EXCHANGE),
            eq(BASIC_PUBLISH_ROUTING_KEY),
            any(),
            eq(BASIC_PUBLISH_MESSAGE.getBytes())
        )
    }

    /**
     * Tests the RPC method given a properties object.
     */
    void testRpcProperties() {
        // Mock for a basic RPC test
        mockBasicRpc(BASIC_RESPONSE_MESSAGE.getBytes())

        // Make the RPC call
        String response = rabbitMessagePublisher.rpc(new RabbitMessageProperties().build {
            exchange = BASIC_PUBLISH_EXCHANGE
            routingKey = BASIC_PUBLISH_ROUTING_KEY
            body = BASIC_PUBLISH_MESSAGE
        })

        // Ensure the correct bits were sent to the publish call
        assert response == BASIC_RESPONSE_MESSAGE : "Expected response \"$BASIC_RESPONSE_MESSAGE\"; received \"$response\""

        // Ensure the correct bits were sent to the publish call
        verify(channel).basicPublish(
            eq(BASIC_PUBLISH_EXCHANGE),
            eq(BASIC_PUBLISH_ROUTING_KEY),
            any(),
            eq(BASIC_PUBLISH_MESSAGE.getBytes())
        )
    }

    /**
     * Tests the RPC method given a closure.
     */
    void testRpcClosure() {
        // Mock for a basic RPC test
        mockBasicRpc(BASIC_RESPONSE_MESSAGE.getBytes())

        // Make the RPC call
        String response = rabbitMessagePublisher.rpc {
            exchange = BASIC_PUBLISH_EXCHANGE
            routingKey = BASIC_PUBLISH_ROUTING_KEY
            body = BASIC_PUBLISH_MESSAGE
        }

        // Ensure the correct bits were sent to the publish call
        assert response == BASIC_RESPONSE_MESSAGE : "Expected response \"$BASIC_RESPONSE_MESSAGE\"; received \"$response\""

        // Ensure the correct bits were sent to the publish call
        verify(channel).basicPublish(
            eq(BASIC_PUBLISH_EXCHANGE),
            eq(BASIC_PUBLISH_ROUTING_KEY),
            any(),
            eq(BASIC_PUBLISH_MESSAGE.getBytes())
        )
    }

    /**
     * Test the timeout functionality for an RPC call.
     */
    void testRpcTimeout() {
        // Mock temporary queue creation
        when(channel.queueDeclare()).thenReturn(new DeclareOk('temporary-queue', 0, 0))

        // Set up the publisher as a spy (we need partial mocking for rpc calls)
        rabbitMessagePublisher = spy(rabbitMessagePublisher)

        // Create a real sync queue for the RPC consumer
        SynchronousQueue<MessageContext> queue = new SynchronousQueue<MessageContext>()
        when(rabbitMessagePublisher.createResponseQueue()).thenReturn(queue)

        // Set the RPC up for failure
        shouldFail(TimeoutException) {
            rabbitMessagePublisher.rpc {
                routingKey = BASIC_PUBLISH_ROUTING_KEY
                timeout = 500
            }
        }
    }
}
