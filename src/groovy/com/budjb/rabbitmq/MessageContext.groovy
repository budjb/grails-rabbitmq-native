package com.budjb.rabbitmq

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope

class MessageContext {
    /**
     * Channel
     */
    Channel channel

    /**
     * Consumer tag.
     */
    String consumerTag

    /**
     * Message envelope
     */
    Envelope envelope

    /**
     * Message properties
     */
    AMQP.BasicProperties properties

    /**
     * Raw message body
     */
    byte[] body
}
