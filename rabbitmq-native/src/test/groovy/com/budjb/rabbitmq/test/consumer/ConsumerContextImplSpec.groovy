/*
 * Copyright 2013-2017 Bud Byrd
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

import com.budjb.rabbitmq.RunningState
import com.budjb.rabbitmq.connection.ConnectionContext
import com.budjb.rabbitmq.connection.ConnectionManager
import com.budjb.rabbitmq.consumer.*
import com.budjb.rabbitmq.converter.*
import com.budjb.rabbitmq.publisher.RabbitMessagePublisher
import com.rabbitmq.client.BasicProperties
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import com.rabbitmq.client.impl.AMQImpl.Queue.DeclareOk
import grails.persistence.support.PersistenceContextInterceptor
import org.slf4j.Logger
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
        messageConverterManager.register(new JsonMessageConverter())
        messageConverterManager.register(new LongMessageConverter())
        messageConverterManager.register(new StringMessageConverter())
    }

    def 'Validate retrieving the context ID'() {
        setup:
        MessageConsumer consumer = Mock(MessageConsumer)
        consumer.getId() >> 'com.budjb.rabbitmq.test.support.UnitTestConsumer'

        when:
        ConsumerContextImpl context = new ConsumerContextImpl(consumer, null, null, null)

        then:
        context.getId() == 'com.budjb.rabbitmq.test.support.UnitTestConsumer'
    }

    def 'Validate retrieving the connection name'() {
        setup:
        MessageConsumer consumer = Mock(MessageConsumer)
        consumer.getConfiguration() >> new ConsumerConfigurationImpl(connection: 'foobar')

        when:
        ConsumerContext context = new ConsumerContextImpl(consumer, null, null, null)

        then:
        context.getConnectionName() == 'foobar'
    }

    def 'Verify that the proper consumer callbacks are invoked for a successful message'() {
        setup:
        ConsumerConfiguration configuration = new ConsumerConfigurationImpl()
        configuration.queue = 'test-queue'

        MessageConsumer consumer = Mock(MessageConsumer)
        consumer.getConfiguration() >> configuration

        ConsumerContextImpl consumerContext = new ConsumerContextImpl(
            consumer,
            connectionManager,
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
        1 * consumer.onReceive(messageContext)
        1 * consumer.onSuccess(messageContext)
        1 * consumer.onComplete(messageContext)
        0 * consumer.onFailure(_)
    }

    def 'Verify that the proper consumer callbacks are invoked for an unsuccessful message'() {
        setup:
        ConsumerConfiguration configuration = new ConsumerConfigurationImpl()
        configuration.queue = 'test-queue'

        MessageConsumer consumer = Mock(MessageConsumer)
        consumer.getConfiguration() >> configuration
        consumer.process(_) >> { throw new RuntimeException() }

        ConsumerContextImpl consumerContext = new ConsumerContextImpl(
            consumer,
            connectionManager,
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
        1 * consumer.onReceive(messageContext)
        0 * consumer.onSuccess(_)
        1 * consumer.onComplete(messageContext)
        1 * consumer.onFailure(messageContext, _)
    }

    def 'Start a basic consumer'() {
        setup:
        ConnectionContext connectionContext = Mock(ConnectionContext)
        connectionContext.getRunningState() >> RunningState.RUNNING
        connectionContext.createChannel(*_) >> {
            return Mock(Channel)
        }

        connectionManager.getContext(*_) >> connectionContext

        ConsumerConfiguration configuration = new ConsumerConfigurationImpl()
        configuration.queue = 'test-queue'
        configuration.consumers = 5

        MessageConsumer consumer = Mock(MessageConsumer)
        consumer.getConfiguration() >> configuration

        ConsumerContextImpl context = new ConsumerContextImpl(
            consumer,
            connectionManager,
            persistenceInterceptor,
            rabbitMessagePublisher
        )

        when:
        context.start()

        then:
        context.getStatusReport().numConfigured == 5
    }

    def 'If the consumer has already been started and tried to start again, throw an IllegalStateException'() {
        setup:
        ConnectionContext connectionContext = Mock(ConnectionContext)
        connectionContext.createChannel(*_) >> {
            return Mock(Channel)
        }
        connectionContext.getRunningState() >> RunningState.RUNNING
        connectionManager.getContext(*_) >> connectionContext

        MessageConsumer consumer = Mock(MessageConsumer)
        consumer.getConfiguration() >> new ConsumerConfigurationImpl(queue: 'foobar')

        ConsumerContextImpl context = new ConsumerContextImpl(
            consumer,
            connectionManager,
            persistenceInterceptor,
            rabbitMessagePublisher
        )

        context.start()
        context.consumers*.runningState = RunningState.RUNNING

        when:
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
        channel.queueDeclare(*_) >> { new DeclareOk('test-queue', 0, 0) }

        ConnectionContext connectionContext = Mock(ConnectionContext)
        connectionContext.getRunningState() >> RunningState.RUNNING
        connectionContext.createChannel(*_) >> channel

        connectionManager.getContext(*_) >> connectionContext

        MessageConsumer consumer = Mock(MessageConsumer)
        consumer.getConfiguration() >> configuration

        ConsumerContextImpl context = new ConsumerContextImpl(
            consumer,
            connectionManager,
            persistenceInterceptor,
            rabbitMessagePublisher
        )

        when:
        context.start()

        then:
        context.getStatusReport().numConfigured == 1
        1 * channel.basicConsume('test-queue', _, _)
    }

    def 'If the persistence interceptor is present, validate its interactions'() {
        setup:
        ConsumerConfiguration configuration = new ConsumerConfigurationImpl()
        configuration.queue = 'test-queue'

        MessageConsumer consumer = Mock(MessageConsumer)
        consumer.getConfiguration() >> configuration

        ConsumerContextImpl consumerContext = new ConsumerContextImpl(
            consumer,
            connectionManager,
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

        MessageConsumer consumer = Mock(MessageConsumer)
        consumer.getConfiguration() >> configuration
        consumer.process(_) >> 'foobar'
        consumer.getId() >> 'com.budjb.rabbitmq.test.support.UnitTestConsumer'

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
            consumer,
            connectionManager,
            persistenceInterceptor,
            rabbitMessagePublisher
        )

        rabbitMessagePublisher.send(*_) >> { throw new StackOverflowError() }

        consumerContext.log = log

        when:
        consumerContext.deliverMessage(messageContext)

        then:
        notThrown StackOverflowError
        1 * log.error('unexpected exception class java.lang.StackOverflowError encountered while responding from an RPC call with handler com.budjb.rabbitmq.test.support.UnitTestConsumer', _)
    }
}
