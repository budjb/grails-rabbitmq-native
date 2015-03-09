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
package com.budjb.rabbitmq.test.consumer

import com.budjb.rabbitmq.connection.ConnectionContext
import com.budjb.rabbitmq.connection.ConnectionManager
import com.budjb.rabbitmq.consumer.ConsumerConfiguration
import com.budjb.rabbitmq.consumer.ConsumerConfigurationImpl
import com.budjb.rabbitmq.consumer.ConsumerContextImpl
import com.budjb.rabbitmq.consumer.MessageContext
import com.budjb.rabbitmq.converter.*
import com.budjb.rabbitmq.publisher.RabbitMessagePublisher
import com.budjb.rabbitmq.test.UnitTestConsumer
import com.rabbitmq.client.BasicProperties
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import com.rabbitmq.client.impl.AMQImpl.Queue.DeclareOk
import org.apache.log4j.Logger
import org.codehaus.groovy.grails.support.PersistenceContextInterceptor
import spock.lang.Specification

class ConsumerContextImplSpec extends Specification {
    MessageConverterManager messageConverterManager
    PersistenceContextInterceptor persistenceInterceptor
    ConnectionManager connectionManager
    RabbitMessagePublisher rabbitMessagePublisher

    def setup() {
        persistenceInterceptor = Mock(PersistenceContextInterceptor)
        connectionManager = Mock(ConnectionManager)
        rabbitMessagePublisher = Mock(RabbitMessagePublisher)

        messageConverterManager = new MessageConverterManagerImpl()
        messageConverterManager.register(new IntegerMessageConverter())
        messageConverterManager.register(new MapMessageConverter())
        messageConverterManager.register(new ListMessageConverter())
        messageConverterManager.register(new GStringMessageConverter())
        messageConverterManager.register(new StringMessageConverter())
    }

    def 'Validate retrieving the consumer name'() {
        setup:
        ConsumerConfiguration configuration = Mock(ConsumerConfiguration)
        UnitTestConsumer consumer = new UnitTestConsumer()

        when:
        ConsumerContextImpl context = new ConsumerContextImpl(configuration, consumer, null, null, null, null)

        then:
        context.getId() == 'UnitTestConsumer'
    }

    def 'Verify that the proper consumer callbacks are invoked for a successful message'() {
        setup:
        ConsumerConfiguration configuration = new ConsumerConfigurationImpl()
        configuration.queue = 'test-queue'

        UnitTestConsumer consumer = Mock(UnitTestConsumer)

        ConsumerContextImpl consumerContext = Spy(ConsumerContextImpl, constructorArgs: [
            configuration,
            consumer,
            connectionManager,
            messageConverterManager,
            persistenceInterceptor,
            rabbitMessagePublisher
        ])

        MessageContext messageContext = new MessageContext(
            channel: Mock(Channel),
            consumerTag: '',
            envelope: Mock(Envelope),
            properties: Mock(BasicProperties),
            body: 'test body'.getBytes(),
            connectionContext: Mock(ConnectionContext)
        )

        when:
        consumerContext.deliverMessage(messageContext)

        then:
        1 * consumer.onReceive(messageContext)
        1 * consumer.onSuccess(messageContext)
        1 * consumer.onComplete(messageContext)
        0 * consumer.onFailure(_)
    }

    def 'Verify that the proper consumer callbacks are invoked for an unsuccessful message'() {
        setup:
        ConsumerConfiguration configuration = new ConsumerConfigurationImpl()
        configuration.queue = 'test-queue'

        UnitTestConsumer consumer = Mock(UnitTestConsumer)
        consumer.handleMessage(*_) >> { throw new RuntimeException() }

        ConsumerContextImpl consumerContext = Spy(ConsumerContextImpl, constructorArgs: [
            configuration,
            consumer,
            connectionManager,
            messageConverterManager,
            persistenceInterceptor,
            rabbitMessagePublisher
        ])

        MessageContext messageContext = new MessageContext(
            channel: Mock(Channel),
            consumerTag: '',
            envelope: Mock(Envelope),
            properties: Mock(BasicProperties),
            body: 'test body'.getBytes(),
            connectionContext: Mock(ConnectionContext)
        )

        when:
        consumerContext.deliverMessage(messageContext)

        then:
        1 * consumer.onReceive(messageContext)
        0 * consumer.onSuccess(_)
        1 * consumer.onComplete(messageContext)
        1 * consumer.onFailure(messageContext)
    }

    def 'Start a basic consumer'() {
        setup:
        ConsumerConfiguration configuration = new ConsumerConfigurationImpl()
        configuration.queue = 'test-queue'
        configuration.consumers = 5

        ConnectionContext connectionContext = Mock(ConnectionContext)
        connectionContext.createChannel(*_) >> {
            return Mock(Channel)
        }

        connectionManager.getContext(*_) >> connectionContext

        UnitTestConsumer consumer = new UnitTestConsumer()

        ConsumerContextImpl context = new ConsumerContextImpl(
            configuration,
            consumer,
            connectionManager,
            messageConverterManager,
            persistenceInterceptor,
            rabbitMessagePublisher
        )

        when:
        context.start()

        then:
        context.consumers.size() == 5
    }

    def 'If the consumer has already been started and tried to start again, throw an IllegalStateException'() {
        setup:
        ConsumerConfiguration configuration = new ConsumerConfigurationImpl()
        configuration.queue = 'test-queue'

        ConnectionContext connectionContext = Mock(ConnectionContext)
        connectionContext.createChannel(*_) >> {
            return Mock(Channel)
        }

        connectionManager.getContext(*_) >> connectionContext

        UnitTestConsumer consumer = new UnitTestConsumer()

        ConsumerContextImpl context = new ConsumerContextImpl(
            configuration,
            consumer,
            connectionManager,
            messageConverterManager,
            persistenceInterceptor,
            rabbitMessagePublisher
        )

        when:
        context.start()
        context.start()

        then:
        thrown IllegalStateException
    }

    def 'If using an exchange and binding, there should only be one consumer created'() {
        setup:
        ConsumerConfiguration configuration = new ConsumerConfigurationImpl()
        configuration.exchange = 'test-exchange'
        configuration.binding = '#'

        Channel channel = Mock(Channel)
        channel.queueDeclare(*_) >> { new DeclareOk('temp-queue', 0, 0) }

        ConnectionContext connectionContext = Mock(ConnectionContext)
        connectionContext.createChannel(*_) >> channel

        connectionManager.getContext(*_) >> connectionContext

        UnitTestConsumer consumer = new UnitTestConsumer()

        ConsumerContextImpl context = new ConsumerContextImpl(
            configuration,
            consumer,
            connectionManager,
            messageConverterManager,
            persistenceInterceptor,
            rabbitMessagePublisher
        )

        when:
        context.start()

        then:
        context.consumers.size() == 1
        1 * channel.basicConsume('temp-queue', _, _)
    }

    def 'If the persistence interceptor is present, validate its interactions'() {
        setup:
        ConsumerConfiguration configuration = new ConsumerConfigurationImpl()
        configuration.queue = 'test-queue'

        UnitTestConsumer consumer = new UnitTestConsumer()

        ConsumerContextImpl consumerContext = new ConsumerContextImpl(
            configuration,
            consumer,
            connectionManager,
            messageConverterManager,
            persistenceInterceptor,
            rabbitMessagePublisher
        )

        MessageContext messageContext = new MessageContext(
            channel: Mock(Channel),
            consumerTag: '',
            envelope: Mock(Envelope),
            properties: Mock(BasicProperties),
            body: 'test body'.getBytes(),
            connectionContext: Mock(ConnectionContext)
        )

        when:
        consumerContext.deliverMessage(messageContext)

        then:
        1 * persistenceInterceptor.init()
        1 * persistenceInterceptor.flush()
        1 * persistenceInterceptor.destroy()
    }


    def 'If an object causes a StackOverflowError during an RPC reply, the exception should be handled'() {
        setup:
        ConsumerConfiguration configuration = new ConsumerConfigurationImpl()
        configuration.queue = 'test-queue'

        Logger log = Mock(Logger)

        UnitTestConsumer consumer = new UnitTestConsumer()

        BasicProperties properties = Mock(BasicProperties)
        properties.getReplyTo() >> 'test-queue'
        properties.getContentType() >> ''

        MessageContext messageContext = new MessageContext(
            channel: Mock(Channel),
            envelope: Mock(Envelope),
            properties: properties,
            body: 'asdf'.getBytes()
        )

        ConsumerContextImpl consumerContext = new ConsumerContextImpl(
            configuration,
            consumer,
            connectionManager,
            messageConverterManager,
            persistenceInterceptor,
            rabbitMessagePublisher
        )

        rabbitMessagePublisher.send(*_) >> { throw new StackOverflowError() }

        consumerContext.log = log

        when:
        consumerContext.deliverMessage(messageContext)

        then:
        notThrown StackOverflowError
        1 * log.error('unexpected exception class java.lang.StackOverflowError encountered while responding from an RPC call with handler UnitTestConsumer', _)
    }
}
