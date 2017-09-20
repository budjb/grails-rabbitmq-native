package com.budjb.rabbitmq.test.support

import com.budjb.rabbitmq.consumer.MessageContext
import com.budjb.rabbitmq.consumer.UnsupportedMessageHandler

class TestUnsupportedMessageConsumer implements UnsupportedMessageHandler {
    static rabbitConfig = [
        queue: 'foo'
    ]

    boolean unsupportedCalled = false

    void handleMessage(BigDecimal body) {

    }

    @Override
    Object handleUnsupportedMessage(MessageContext messageContext) {
        unsupportedCalled = true
    }
}
