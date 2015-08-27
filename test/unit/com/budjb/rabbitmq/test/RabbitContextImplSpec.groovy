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

import com.budjb.rabbitmq.RunningState
import com.budjb.rabbitmq.QueueBuilder
import com.budjb.rabbitmq.RabbitContextImpl
import com.budjb.rabbitmq.connection.ConnectionConfiguration
import com.budjb.rabbitmq.connection.ConnectionContext
import com.budjb.rabbitmq.connection.ConnectionManager
import com.budjb.rabbitmq.consumer.ConsumerContext
import com.budjb.rabbitmq.consumer.ConsumerManager
import com.budjb.rabbitmq.converter.MessageConverter
import com.budjb.rabbitmq.converter.MessageConverterManager
import org.codehaus.groovy.grails.commons.GrailsApplication
import spock.lang.Specification
import spock.lang.Unroll

class RabbitContextImplSpec extends Specification {
    GrailsApplication grailsApplication
    MessageConverterManager messageConverterManager
    ConnectionManager connectionManager
    ConsumerManager consumerManager
    RabbitContextImpl rabbitContext
    QueueBuilder queueBuilder

    def setup() {
        grailsApplication = Mock(GrailsApplication)
        messageConverterManager = Mock(MessageConverterManager)
        connectionManager = Mock(ConnectionManager)
        consumerManager = Mock(ConsumerManager)
        queueBuilder = Mock(QueueBuilder)

        rabbitContext = new RabbitContextImpl()
        rabbitContext.setMessageConverterManager(messageConverterManager)
        rabbitContext.setConnectionManager(connectionManager)
        rabbitContext.setConsumerManager(consumerManager)
        rabbitContext.setQueueBuilder(queueBuilder)
    }

    def 'Ensure createChannel() is proxied through to the connectionManager'() {
        when:
        rabbitContext.createChannel()

        then:
        1 * connectionManager.createChannel()
    }

    def 'Ensure createChannel(String) is proxied through to the connection manager'() {
        when:
        rabbitContext.createChannel('test-connection')

        then:
        1 * connectionManager.createChannel('test-connection')
    }

    def 'Ensure getConnection() is proxied through to the connection manager'() {
        when:
        rabbitContext.getConnection()

        then:
        1 * connectionManager.getConnection()
    }

    def 'Ensure getConnection(String) is proxied through to the connection manager'() {
        when:
        rabbitContext.getConnection("test")

        then:
        1 * connectionManager.getConnection("test")
    }

    def 'When load() is called, all of the inject managers should be loaded'() {
        when:
        rabbitContext.load()

        then:
        1 * connectionManager.load()
        1 * messageConverterManager.load()
        1 * consumerManager.load()
    }

    def 'Ensure registerConsumer(Object) is proxied to the rabbit consumer manager'() {
        setup:
        Object consumer = Mock(Object)
        ConsumerContext context = Mock(ConsumerContext)

        consumerManager.createContext(consumer) >> context

        when:
        rabbitContext.registerConsumer(consumer)

        then:
        1 * consumerManager.register(context)
    }

    def 'Ensure registerMessageConverter(MessageConverter) is proxied to the rabbit consumer manager'() {
        setup:
        MessageConverter<?> converter = Mock(MessageConverter)

        when:
        rabbitContext.registerMessageConverter(converter)

        then:
        1 * messageConverterManager.register(converter)
    }

    def 'When stop() is called, the connection and message converter managers should be stopped'() {
        when:
        rabbitContext.stop()

        then:
        1 * connectionManager.stop()
        1 * consumerManager.stop()
    }

    def 'When start() is called, connections should be opened, queues should be configured, and consumers should be started'() {
        when:
        rabbitContext.start()

        then:
        1 * connectionManager.start()
        1 * queueBuilder.configureQueues()
        1 * consumerManager.start()
    }

    def 'When start(true) is called, connections should be opened, queues should be configured, and consumers should not be started'() {
        when:
        rabbitContext.start(true)

        then:
        1 * connectionManager.start()
        1 * queueBuilder.configureQueues()
        0 * consumerManager.start()
    }

    def 'Ensure the proper interactions for reload (stop/load/start)'() {
        when:
        rabbitContext.reload()

        then:
        1 * connectionManager.stop()
        1 * consumerManager.stop()

        then:
        1 * connectionManager.load()
        1 * messageConverterManager.load()
        1 * consumerManager.load()

        then:
        1 * connectionManager.start()
        1 * queueBuilder.configureQueues()
        1 * consumerManager.start()
    }

    def 'Ensure startConsumers() is proxied to the consumer manager'() {
        when:
        rabbitContext.startConsumers()

        then:
        1 * consumerManager.start()
    }

    def 'Ensure stopConsumers() is proxied to the consumer manager'() {
        when:
        rabbitContext.stopConsumers()

        then:
        1 * consumerManager.stop()
    }

    def 'Ensure startConsumers(String) is proxied to the consumer manager with the correct connection context'() {
        setup:
        ConnectionContext connectionContext = Mock(ConnectionContext)
        connectionManager.getContext('connection') >> connectionContext

        when:
        rabbitContext.startConsumers('connection')

        then:
        1 * consumerManager.start(connectionContext)
    }

    def 'Ensure stopConsumers(String) is proxied to the consumer manager with the correct connection context'() {
        setup:
        ConnectionContext connectionContext = Mock(ConnectionContext)
        connectionManager.getContext('connection') >> connectionContext

        when:
        rabbitContext.stopConsumers('connection')

        then:
        1 * consumerManager.stop(connectionContext)
    }

    def 'Ensure startConsumer(String) is proxied to the consumer manager'() {
        when:
        rabbitContext.startConsumer('consumer')

        then:
        1 * consumerManager.start('consumer')
    }

    def 'Ensure stopConsumer(String) is proxied to the consumer manager'() {
        when:
        rabbitContext.stopConsumer('consumer')

        then:
        1 * consumerManager.stop('consumer')
    }

    def 'Ensure startConnections() is proxied to the connection manager'() {
        when:
        rabbitContext.startConnections()

        then:
        1 * connectionManager.start()
    }

    def 'Ensure startConnection(String) is proxied to the connection manager'() {
        when:
        rabbitContext.startConnection('connection')

        then:
        1 * connectionManager.start('connection')
    }

    def 'Ensure stopConnections() is proxied to the connection manager'() {
        when:
        rabbitContext.stopConnections()

        then:
        1 * connectionManager.stop()
    }

    def 'Ensure stopConnection(String) is proxied to the connection manager'() {
        setup:
        connectionManager.getContext('connection') >> Mock(ConnectionContext)

        when:
        rabbitContext.stopConnection('connection')

        then:
        1 * connectionManager.stop('connection')
    }

    def 'Ensure registerConnection(ConnectionConfiguration) is proxied to the connection manager'() {
        setup:
        ConnectionConfiguration configuration = Mock(ConnectionConfiguration)
        ConnectionContext context = Mock(ConnectionContext)

        connectionManager.createContext(configuration) >> context

        when:
        rabbitContext.registerConnection(configuration)

        then:
        1 * connectionManager.register(context)
    }

    def 'Ensure createExchangesAndQueues is proxied to the queue builder'() {
        when:
        rabbitContext.createExchangesAndQueues()

        then:
        1 * queueBuilder.configureQueues()
    }

    def 'Ensure all managers are reset when reset() is called'() {
        when:
        rabbitContext.reset()

        then:
        1 * consumerManager.reset()
        1 * connectionManager.reset()
        1 * messageConverterManager.reset()
    }

    def 'When shutdown() is called, the consumer manager is shut down and connection manager is stopped'() {
        when:
        rabbitContext.shutdown()

        then:
        1 * consumerManager.shutdown()
        1 * connectionManager.stop()
    }

    @Unroll
    def 'When consumerManager has state #consumer and connection manager has state #connection, state #result is returned'() {
        setup:
        consumerManager.getRunningState() >> consumer
        connectionManager.getRunningState() >> connection

        when:
        RunningState resultState = rabbitContext.getRunningState()

        then:
        resultState == result
        (consumersStopped) * consumerManager.stop()

        where:
        consumer                    | connection            | consumersStopped  | result
        RunningState.RUNNING        | RunningState.RUNNING  | 0                 | RunningState.RUNNING
        RunningState.STOPPED        | RunningState.RUNNING  | 0                 | RunningState.RUNNING
        RunningState.SHUTTING_DOWN  | RunningState.RUNNING  | 0                 | RunningState.SHUTTING_DOWN
        RunningState.RUNNING        | RunningState.STOPPED  | 1                 | RunningState.STOPPED
        RunningState.STOPPED        | RunningState.STOPPED  | 0                 | RunningState.STOPPED
        RunningState.SHUTTING_DOWN  | RunningState.STOPPED  | 1                 | RunningState.STOPPED
    }
}
