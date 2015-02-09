package com.budjb.rabbitmq.test

import com.budjb.rabbitmq.consumer.MessageContext

class AllTopicConsumer {
    static rabbitConfig = [
        connection: 'connection1',
        queue: 'topic-queue-all'
    ]

    public Map lastMessage

    String handleMessage(String body, MessageContext messageContext) {
        recordLastRequest('String', body, messageContext)

        return body
    }

    Integer handleMessage(Integer body, MessageContext messageContext) {
        recordLastRequest('Integer', body, messageContext)

        return body
    }

    List handleMessage(List body, MessageContext messageContext) {
        recordLastRequest('List', body, messageContext)

        return body
    }

    Map handleMessage(Map body, MessageContext messageContext) {
        recordLastRequest('Map', body, messageContext)

        return body
    }

    Object handleMessage(Object body, MessageContext messageContext) {
        recordLastRequest(body.getClass().toString(), body, messageContext)

        return body
    }

    private void recordLastRequest(String type, Object body, MessageContext messageContext) {
        lastMessage = [
            type: type,
            body: body,
            messageContext: messageContext
        ]
    }
}
