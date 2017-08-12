package com.budjb.rabbitmq.test.consumer

import com.budjb.rabbitmq.exception.MissingConfigurationException
import com.budjb.rabbitmq.test.support.MissingConfigurationConsumer
import com.budjb.rabbitmq.test.support.UnitTestConsumer
import grails.config.Config
import grails.core.GrailsApplication
import org.grails.config.PropertySourcesConfig
import spock.lang.Specification

class GrailsMessageConsumerSpec extends Specification {
    def 'If a consumer has a configuration defined in the application config, it is loaded correctly'() {
        setup:
        Config config = new PropertySourcesConfig()
        config.putAll([
            rabbitmq: [
                consumers: [
                    'UnitTestConsumer': [
                        queue    : 'test-queue',
                        consumers: 10
                    ]
                ]
            ]
        ])

        GrailsApplication grailsApplication = Mock(GrailsApplication)
        grailsApplication.getConfig() >> config

        UnitTestConsumer consumer = new UnitTestConsumer()
        consumer.grailsApplication = grailsApplication
        consumer.afterPropertiesSet()

        expect:
        consumer.id == 'com.budjb.rabbitmq.test.support.UnitTestConsumer'
        consumer.configuration.queue == 'test-queue'
        consumer.configuration.consumers == 10
    }

    def 'If a consumer has a configuration defined within the object, it is loaded correctly'() {
        setup:
        GrailsApplication grailsApplication = Mock(GrailsApplication)
        grailsApplication.getConfig() >> new PropertySourcesConfig()

        UnitTestConsumer consumer = new UnitTestConsumer()
        consumer.grailsApplication = grailsApplication
        consumer.afterPropertiesSet()

        expect:
        consumer.id == 'com.budjb.rabbitmq.test.support.UnitTestConsumer'
        consumer.configuration.queue == 'test-queue'
        consumer.configuration.consumers == 5
    }

    def 'If a consumer has no configuration defined, a MissingConfigurationException is thrown'() {
        setup:
        GrailsApplication grailsApplication = Mock(GrailsApplication)
        grailsApplication.getConfig() >> new PropertySourcesConfig()

        MissingConfigurationConsumer consumer = new MissingConfigurationConsumer()
        consumer.grailsApplication = grailsApplication

        when:
        consumer.afterPropertiesSet()

        then:
        thrown MissingConfigurationException
    }
}
