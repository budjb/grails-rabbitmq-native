package com.budjb.rabbitmq.test

import grails.test.mixin.integration.Integration
import spock.lang.Specification

@Integration
class MessageContextSpec extends MessageConsumerIntegrationTest {
    MessageContextConsumer messageContextConsumer

    def 'When a consumer only has a single-parameter handler for MessageContext, message are successfully delivered'() {
        setup:
        messageContextConsumer.received = false

        when:
        rabbitMessagePublisher.send {
            routingKey = 'message-context'
            body = 'ignored'
        }

        sleep 5000

        then:
        messageContextConsumer.received == true
    }
}
