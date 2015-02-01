package com.budjb.rabbitmq.test

import grails.test.mixin.support.GrailsUnitTestMixin

import java.util.concurrent.SynchronousQueue

import org.junit.Before

import static org.mockito.Mockito.*

import com.budjb.rabbitmq.MessageContext
import com.budjb.rabbitmq.RabbitContext
import com.budjb.rabbitmq.RabbitMessagePublisher
import com.budjb.rabbitmq.converter.GStringMessageConverter
import com.budjb.rabbitmq.converter.IntegerMessageConverter
import com.budjb.rabbitmq.converter.ListMessageConverter
import com.budjb.rabbitmq.converter.MapMessageConverter
import com.budjb.rabbitmq.converter.StringMessageConverter
import com.rabbitmq.client.Channel
import com.rabbitmq.client.impl.AMQImpl.Queue.DeclareOk

class RabbitMessagePublisherTest extends GrailsUnitTestMixin {
    /**
     * Mocked rabbit context
     */
    RabbitContext rabbitContext

    /**
     * Live message publisher instance
     */
    RabbitMessagePublisher rabbitMessagePublisher

    /**
     * Mocked channel
     */
    Channel channel

    /**
     * Sets up the basic requirements for a mocked RabbitMQ system.
     */
    @Before
    public void setup() {
        // Mock the rabbit context
        rabbitContext = mock(RabbitContext)

        // Mock/inject message converters
        when(rabbitContext.getMessageConverters()).thenReturn([
            new IntegerMessageConverter(),
            new MapMessageConverter(),
            new ListMessageConverter(),
            new GStringMessageConverter(),
            new StringMessageConverter()
        ])

        // Mock a channel
        channel = mock(Channel)

        // Mock a channel return from the rabbit context
        when(rabbitContext.createChannel(null)).thenReturn(channel)

        // Create the message publisher
        rabbitMessagePublisher = new RabbitMessagePublisher()
        rabbitMessagePublisher.rabbitContext = rabbitContext
    }

    /**
     * Sets up a basic RPC test.
     *
     * @param response
     */
    protected void mockBasicRpc(byte[] response) {
        // Mock temporary queue creation
        when(channel.queueDeclare()).thenReturn(new DeclareOk('temporary-queue', 0, 0))

        // Set up the publisher as a spy (we need partial mocking for rpc calls)
        rabbitMessagePublisher = spy(rabbitMessagePublisher)

        // Create a mocked response message context
        MessageContext responseMessageContext = new MessageContext(
            channel: null,
            consumerTag: null,
            envelope: null,
            properties: null,
            body: response
        )

        // Mock a sync queue for the rpc consumer
        SynchronousQueue<MessageContext> queue = mock(SynchronousQueue)
        when(rabbitMessagePublisher.createResponseQueue()).thenReturn(queue)

        // Mock the poll
        when(queue.poll(anyLong(), any())).thenReturn(responseMessageContext)
        when(queue.take()).thenReturn(responseMessageContext)
    }
}
