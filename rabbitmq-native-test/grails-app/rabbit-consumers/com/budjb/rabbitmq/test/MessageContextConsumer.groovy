package com.budjb.rabbitmq.test

import com.budjb.rabbitmq.consumer.MessageContext

class MessageContextConsumer {
    static rabbitConfig = [
        queue: 'message-context'
    ]

    boolean received = false

    void handleMessage(MessageContext messageContext) {
        received = true
    }
}
