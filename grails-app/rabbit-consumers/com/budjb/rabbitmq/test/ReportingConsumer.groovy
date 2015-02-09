package com.budjb.rabbitmq.test

import com.budjb.rabbitmq.consumer.MessageContext

class ReportingConsumer {
    static rabbitConfig = [
        connection: 'connection1',
        queue: 'reporting'
    ]

    public Map lastMessage

    void handleMessage(String body, MessageContext messageContext) {
        recordLastRequest('String', body, messageContext)
    }

    void handleMessage(Integer body, MessageContext messageContext) {
        recordLastRequest('Integer', body, messageContext)
    }

    void handleMessage(List body, MessageContext messageContext) {
        recordLastRequest('List', body, messageContext)
    }

    void handleMessage(Map body, MessageContext messageContext) {
        recordLastRequest('Map', body, messageContext)
    }

    void handleMessage(Object body, MessageContext messageContext) {
        recordLastRequest(body.getClass().toString(), body, messageContext)
    }

    private void recordLastRequest(String type, Object body, MessageContext messageContext) {
        lastMessage = [
            type: type,
            body: body,
            messageContext: messageContext
        ]
    }
}
