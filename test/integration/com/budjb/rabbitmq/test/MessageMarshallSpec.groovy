package com.budjb.rabbitmq.test

import org.codehaus.groovy.grails.commons.GrailsApplication

import com.budjb.rabbitmq.RabbitMessagePublisher

class MessageMarshallSpec extends MessageConsumerIntegrationTest {
    ReportingConsumer reportingConsumer

    def setup() {
        reportingConsumer.lastMessage = null
    }

    def 'If a String is sent to the consumer, ensure the correct handler is called'() {
        when:
        rabbitMessagePublisher.send {
            routingKey = 'reporting'
            body = "foobar"
        }
        Map received = waitUntilMessageReceived(30000) { reportingConsumer.lastMessage }

        then:
        received != null
        received.type == 'String'
        received.body.getClass() == String
        received.body == 'foobar'
    }

    def 'If an Integer is sent to the consumer, ensure the correct handler is called'() {
        when:
        rabbitMessagePublisher.send {
            routingKey = 'reporting'
            body = 1234
        }
        Map received = waitUntilMessageReceived(30000) { reportingConsumer.lastMessage }

        then:
        received != null
        received.type == 'Integer'
        received.body.getClass() == Integer
        received.body == 1234
    }

    def 'If a List is sent to the consumer, ensure the correct handler is called'() {
        when:
        rabbitMessagePublisher.send {
            routingKey = 'reporting'
            body = ["foo", "bar"]
        }
        Map received = waitUntilMessageReceived(30000) { reportingConsumer.lastMessage }

        then:
        received != null
        received.type == 'List'
        received.body instanceof List
        received.body == ["foo", "bar"]
    }

    def 'If a Map is sent to the consumer, ensure the correct handler is called'() {
        when:
        rabbitMessagePublisher.send {
            routingKey = 'reporting'
            body = ["foo": "bar"]
        }
        Map received = waitUntilMessageReceived(30000) { reportingConsumer.lastMessage }

        then:
        received != null
        received.type == 'Map'
        received.body instanceof Map
        received.body == ["foo": "bar"]
    }

    def 'Ensure RPC calls marshall Strings correctly'() {
        when:
        String response = rabbitMessagePublisher.rpc {
            routingKey = 'reporting'
            body = 'foobar'
        }

        then:
        response == 'foobar'
    }

    def 'Ensure RPC calls marshall Integers correctly'() {
        when:
        Integer response = rabbitMessagePublisher.rpc {
            routingKey = 'reporting'
            body = 1234
        }

        then:
        response == 1234
    }

    def 'Ensure RPC calls marshall Lists correctly'() {
        when:
        List response = rabbitMessagePublisher.rpc {
            routingKey = 'reporting'
            body = ["foo", "bar"]
        }

        then:
        response == ["foo", "bar"]
    }

    def 'Ensure RPC calls marshall Maps correctly'() {
        when:
        Map response = rabbitMessagePublisher.rpc {
            routingKey = 'reporting'
            body = ["foo": "bar"]
        }

        then:
        response == ["foo": "bar"]
    }
}
