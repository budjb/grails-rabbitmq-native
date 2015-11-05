/*
 * Copyright 2015 Bud Byrd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.budjb.rabbitmq.test

import com.budjb.rabbitmq.RabbitContext
import com.budjb.rabbitmq.RunningState
import grails.test.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired

import java.util.concurrent.TimeoutException

@Integration
class StartStopConsumerSpec extends MessageConsumerIntegrationTest {
    @Autowired
    ReportingConsumer reportingConsumer

    @Autowired
    RabbitContext rabbitContext

    @Autowired
    Connection2Consumer connection2Consumer

    def 'Test consumer behavior when a specific consumer is stopped and started again'() {
        setup:
        rabbitContext.stopConsumer('ReportingConsumer')
        reportingConsumer.lastMessage = null

        when:
        rabbitMessagePublisher.send {
            routingKey = 'reporting'
            body = 'hello'
        }
        waitUntilMessageReceived(5000) { reportingConsumer.lastMessage }

        then:
        thrown TimeoutException

        when:
        // This is necessary because the configuration for this queue is autoDelete
        // and it's deleted when we stop the consumer.
        rabbitContext.createExchangesAndQueues()

        rabbitContext.startConsumer('ReportingConsumer')
        rabbitMessagePublisher.send {
            routingKey = 'reporting'
            body = 'hello'
        }
        def response = waitUntilMessageReceived(5000) { reportingConsumer.lastMessage }

        then:
        notThrown TimeoutException
        response.body == 'hello'
    }

    def 'Test consumer behavior when stopping and starting an individual connection'() {
        setup:
        rabbitContext.stopConnection('connection1')
        reportingConsumer.lastMessage = null

        when:
        rabbitMessagePublisher.send {
            routingKey = 'reporting'
            body = 'hello'
        }
        waitUntilMessageReceived(5000) { reportingConsumer.lastMessage }

        then:
        // Because the connection's closed :)
        thrown IllegalStateException

        when:
        rabbitMessagePublisher.send {
            routingKey = 'connection2-queue'
            body = 'foo'
            connection = 'connection2'
        }
        waitUntilMessageReceived(5000) { connection2Consumer.lastMessage }

        then:
        notThrown TimeoutException

        when:
        rabbitContext.startConnections()

        // This is necessary because the configuration for this queue is autoDelete
        // and it's deleted when we stop the consumer.
        rabbitContext.createExchangesAndQueues()

        rabbitContext.startConsumers()
        rabbitMessagePublisher.send {
            routingKey = 'reporting'
            body = 'hello'
        }
        def response = waitUntilMessageReceived(5000) { reportingConsumer.lastMessage }

        then:
        notThrown TimeoutException
        response.body == 'hello'
    }

    def 'Test behavior when a consumer is stopped and then all consumers are started again'() {
        setup:
        rabbitContext.stopConsumer('ReportingConsumer')
        reportingConsumer.lastMessage = null

        when:
        rabbitMessagePublisher.send {
            routingKey = 'reporting'
            body = 'hello'
        }
        waitUntilMessageReceived(5000) { reportingConsumer.lastMessage }

        then:
        thrown TimeoutException

        when:
        // This is necessary because the configuration for this queue is autoDelete
        // and it's deleted when we stop the consumer.
        rabbitContext.createExchangesAndQueues()

        rabbitContext.startConsumers()
        rabbitMessagePublisher.send {
            routingKey = 'reporting'
            body = 'hello'
        }
        def response = waitUntilMessageReceived(5000) { reportingConsumer.lastMessage }

        then:
        notThrown TimeoutException
        response.body == 'hello'
    }

    def 'When no messages are being consumed, shutting down should immediately stop consuming'() {
        setup:
        Thread thread = new Thread(new ShutdownRunnable(rabbitContext))

        expect:
        rabbitContext.getRunningState() == RunningState.RUNNING

        when:
        thread.start()
        sleep(500)

        then:
        rabbitContext.getRunningState() == RunningState.STOPPED

        when:
        thread.join()
        rabbitContext.start()

        then:
        rabbitContext.getRunningState() == RunningState.RUNNING
    }

    def 'When messages are being consumed, shutting down should wait until in-flight messages are complete'() {
        setup:
        Thread thread = new Thread(new ShutdownRunnable(rabbitContext))

        expect:
        rabbitContext.getRunningState() == RunningState.RUNNING

        when:
        rabbitMessagePublisher.send {
            routingKey = 'sleeping'
            body = 10000
        }

        thread.start()

        sleep(1000)

        then:
        rabbitContext.getRunningState() == RunningState.SHUTTING_DOWN

        when:
        sleep(1000)

        then:
        rabbitContext.getRunningState() == RunningState.SHUTTING_DOWN

        when:
        thread.join()

        then:
        rabbitContext.getRunningState() == RunningState.STOPPED

        when:
        rabbitContext.start()

        then:
        rabbitContext.getRunningState() == RunningState.RUNNING
    }
}
