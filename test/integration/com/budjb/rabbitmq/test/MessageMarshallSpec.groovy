package com.budjb.rabbitmq.test

import grails.plugin.spock.IntegrationSpec

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.junit.Test

import com.budjb.rabbitmq.RabbitMessagePublisher

class MessageMarshallSpec extends IntegrationSpec {
    RabbitMessagePublisher rabbitMessagePublisher
    ReportingConsumer reportingConsumer
    GrailsApplication grailsApplication

    def cleanup() {
        reportingConsumer.lastMessage = null
    }

    def waitUntilMessageReceived(int timeout) {
        long start = System.currentTimeMillis()
        while (System.currentTimeMillis() < start + timeout) {
            if (reportingConsumer.lastMessage != null) {
                break
            }
            sleep(1000)
        }
    }

    def 'If a String is sent to the consumer, ensure the correct handler is called'() {
        when:
        rabbitMessagePublisher.send {
            routingKey = 'reporting'
            body = "foobar"
        }
        waitUntilMessageReceived(30000)

        then:
        reportingConsumer.lastMessage != null
        reportingConsumer.lastMessage.type == 'String'
        reportingConsumer.lastMessage.body.getClass() == String
        reportingConsumer.lastMessage.body == 'foobar'
    }

    def 'If an Integer is sent to the consumer, ensure the correct handler is called'() {
        when:
        rabbitMessagePublisher.send {
            routingKey = 'reporting'
            body = 1234
        }
        waitUntilMessageReceived(30000)

        then:
        reportingConsumer.lastMessage != null
        reportingConsumer.lastMessage.type == 'Integer'
        reportingConsumer.lastMessage.body.getClass() == Integer
        reportingConsumer.lastMessage.body == 1234
    }

    def 'If a List is sent to the consumer, ensure the correct handler is called'() {
        when:
        rabbitMessagePublisher.send {
            routingKey = 'reporting'
            body = ["foo", "bar"]
        }
        waitUntilMessageReceived(30000)

        then:
        reportingConsumer.lastMessage != null
        reportingConsumer.lastMessage.type == 'List'
        reportingConsumer.lastMessage.body instanceof List
        reportingConsumer.lastMessage.body == ["foo", "bar"]
    }

    def 'If a Map is sent to the consumer, ensure the correct handler is called'() {
        when:
        rabbitMessagePublisher.send {
            routingKey = 'reporting'
            body = ["foo": "bar"]
        }
        waitUntilMessageReceived(30000)

        then:
        reportingConsumer.lastMessage != null
        reportingConsumer.lastMessage.type == 'Map'
        reportingConsumer.lastMessage.body instanceof Map
        reportingConsumer.lastMessage.body == ["foo": "bar"]
    }
}
