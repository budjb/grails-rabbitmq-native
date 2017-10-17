package com.budjb.rabbitmq.test

import grails.util.TypeConvertingMap

class MessageConverterConsumer {
    static rabbitConfig = [
        queue: 'message-converter'
    ]

    Class<?> handler = null

    void handleMessage(Map body) {
        handler = Map
    }

    void handleMessage(TypeConvertingMap body) {
        handler = TypeConvertingMap
    }
}
