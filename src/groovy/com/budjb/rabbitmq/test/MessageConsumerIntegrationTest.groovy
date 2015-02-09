package com.budjb.rabbitmq.test

import java.util.concurrent.TimeoutException

import com.budjb.rabbitmq.RabbitMessagePublisher

import grails.plugin.spock.IntegrationSpec

class MessageConsumerIntegrationTest extends IntegrationSpec {
    /**
     * Rabbit message publisher bean.
     */
    RabbitMessagePublisher rabbitMessagePublisher

    /**
     * Waits for a consumer to receive a message.
     *
     * @param timeout Time, in milliseconds, to wait for the message to be received.
     * @param closure Close that contains the logic to determine if the consumer has received the message.
     * @return
     */
    def waitUntilMessageReceived(int timeout, Closure closure) throws TimeoutException {
        long start = System.currentTimeMillis()
        while (System.currentTimeMillis() < start + timeout) {
            def value = closure()
            if (value != null) {
                return value
            }
            sleep(1000)
        }
        throw new TimeoutException("no message received in ${timeout} milliseconds")
    }
}
