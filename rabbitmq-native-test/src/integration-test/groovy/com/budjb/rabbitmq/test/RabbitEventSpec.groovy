package com.budjb.rabbitmq.test

import grails.test.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

@Integration
class RabbitEventSpec extends Specification {
    @Autowired
    ConsumerManagerCheckListener consumerManagerCheckListener

    def 'When consumers are started, ConsumerManagerStartingEvent listeners finish execution before consumers are started'() {
        expect:
        consumerManagerCheckListener.started != null
        !consumerManagerCheckListener.started
    }
}
