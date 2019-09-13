package com.budjb.rabbitmq.test

import com.budjb.rabbitmq.publisher.RabbitMessagePublisher
import grails.testing.mixin.integration.Integration
import grails.util.TypeConvertingMap
import org.springframework.beans.factory.annotation.Autowired

@Integration
class MessageConverterIntegrationSpec extends MessageConsumerIntegrationTest {
    @Autowired
    RabbitMessagePublisher rabbitMessagePublisher

    @Autowired
    MessageConverterConsumer messageConverterConsumer

    def 'When a TypeConvertingMap is published to a consumer, the TypeConvertingMapMessageConverter is used'() {
        setup:
        TypeConvertingMap map = new TypeConvertingMap([foo: 'bar'])

        when:
        rabbitMessagePublisher.send {
            routingKey = 'message-converter'
            body = map
        }

        Class<?> clazz = (Class<?>) waitUntilMessageReceived(50000) { messageConverterConsumer.handler }

        then:
        clazz == TypeConvertingMap
    }
}
