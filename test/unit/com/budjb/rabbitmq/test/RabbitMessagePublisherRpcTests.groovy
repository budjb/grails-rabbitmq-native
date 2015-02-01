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
    private final String BASIC_PUBLISH_MESSAGE = 'Knock knock...'

    /**
     * Message to respond with in the basic RPC test.
     */
    private final String BASIC_RESPONSE_MESSAGE = 'Who\'s there?'

    /**
     * Tests the RPC method given a routing key and body.
     */
    void testRpcRoutingKey() {
        mockBasicRpc(BASIC_RESPONSE_MESSAGE.getBytes())
        String response = rabbitMessagePublisher.rpc('test-queue', BASIC_PUBLISH_MESSAGE)
        assert response == BASIC_RESPONSE_MESSAGE : "Expected response \"$BASIC_RESPONSE_MESSAGE\"; received \"$response\""
    }

    /**
     * Tests the RPC method given an exchange, routing key, and body.
     */
    void testRpcExchangeAndRoutingKey() {
        mockBasicRpc(BASIC_RESPONSE_MESSAGE.getBytes())
        String response = rabbitMessagePublisher.rpc('test-exchange', 'test-routing-key', BASIC_PUBLISH_MESSAGE)
        assert response == BASIC_RESPONSE_MESSAGE : "Expected response \"$BASIC_RESPONSE_MESSAGE\"; received \"$response\""
    }

    /**
     * Tests the RPC method given a properties object.
     */
    void testRpcProperties() {
        mockBasicRpc(BASIC_RESPONSE_MESSAGE.getBytes())
        String response = rabbitMessagePublisher.rpc(new RabbitMessageProperties().build {
            exchange = 'test-exchange'
            routingKey = 'test-routing-key'
            body = BASIC_PUBLISH_MESSAGE
        })
        assert response == BASIC_RESPONSE_MESSAGE : "Expected response \"$BASIC_RESPONSE_MESSAGE\"; received \"$response\""
    }

    /**
     * Tests the RPC method given a closure.
     */
    void testRpcClosure() {
        mockBasicRpc(BASIC_RESPONSE_MESSAGE.getBytes())
        String response = rabbitMessagePublisher.rpc {
            exchange = 'test-exchange'
            routingKey = 'test-routing-key'
            body = BASIC_PUBLISH_MESSAGE
        }
        assert response == BASIC_RESPONSE_MESSAGE : "Expected response \"$BASIC_RESPONSE_MESSAGE\"; received \"$response\""
    }

    /**
     * Test the timeout functionality for an RPC call.
     */
    void testRpcTimeout() {
        // Mock temporary queue creation
        when(channel.queueDeclare()).thenReturn(new DeclareOk('temporary-queue', 0, 0))

        // Set up the publisher as a spy (we need partial mocking for rpc calls)
        rabbitMessagePublisher = spy(rabbitMessagePublisher)

        // Create a real sync queue for the rpc consumer
        SynchronousQueue<MessageContext> queue = new SynchronousQueue<MessageContext>()
        when(rabbitMessagePublisher.createResponseQueue()).thenReturn(queue)

        // Set the RPC up for failure
        shouldFail(TimeoutException) {
            rabbitMessagePublisher.rpc {
                routingKey = 'test-routing-key'
                timeout = 500
            }
        }
    }
}
