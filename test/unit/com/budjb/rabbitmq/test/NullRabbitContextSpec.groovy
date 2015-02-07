package com.budjb.rabbitmq.test

import com.budjb.rabbitmq.NullRabbitContext

import spock.lang.Specification

class NullRabbitContextSpec extends Specification {
    NullRabbitContext rabbitContext

    def setup() {
        rabbitContext = new NullRabbitContext()
    }

    def 'If createChannel() is called, an UnsupportedOperationException should be thrown'() {
        when:
        rabbitContext.createChannel()

        then:
        thrown UnsupportedOperationException
    }

    def 'If createChannel(String) is called, an UnsupportedOperationException should be thrown'() {
        when:
        rabbitContext.createChannel("test")

        then:
        thrown UnsupportedOperationException
    }

    def 'If getConnection() is called, an UnsupportedOperationException should be thrown'() {
        when:
        rabbitContext.getConnection()

        then:
        thrown UnsupportedOperationException
    }

    def 'If getConnection(String) is called, an UnsupportedOperationException should be thrown'() {
        when:
        rabbitContext.getConnection("test")

        then:
        thrown UnsupportedOperationException
    }
}
