package com.budjb.rabbitmq.test

import com.budjb.rabbitmq.test.helper.MessageConsumerIntegrationTest

class RpcSpec extends MessageConsumerIntegrationTest {
    SpecificTopicConsumer specificTopicConsumer

    def 'If a message is sent directly to a queue based on its name, it should be received'() {
        when:
        def result = rabbitMessagePublisher.rpc {
            routingKey = 'topic-queue-specific'
            body = 'test'
        }

        then:
        result == 'test'
    }
}
