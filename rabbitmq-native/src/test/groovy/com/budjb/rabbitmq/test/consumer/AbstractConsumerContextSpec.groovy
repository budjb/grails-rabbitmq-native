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
import com.budjb.rabbitmq.event.ConsumerContextStartedEvent
import com.budjb.rabbitmq.event.ConsumerContextStartingEvent
import com.budjb.rabbitmq.event.ConsumerContextStoppedEvent
import com.budjb.rabbitmq.event.ConsumerContextStoppingEvent
import com.budjb.rabbitmq.exception.UnsupportedMessageException
import com.budjb.rabbitmq.publisher.RabbitMessageProperties
import com.budjb.rabbitmq.publisher.RabbitMessagePublisher
import com.rabbitmq.client.BasicProperties
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Consumer
import com.rabbitmq.client.Envelope
import com.rabbitmq.client.impl.AMQImpl.Queue.DeclareOk
import grails.persistence.support.PersistenceContextInterceptor
import groovy.transform.InheritConstructors
import org.slf4j.Logger
import org.springframework.context.ApplicationEventPublisher
import spock.lang.Specification

class AbstractConsumerContextSpec extends Specification {
    MessageConverterManager messageConverterManager
    PersistenceContextInterceptor persistenceInterceptor
    ConnectionManager connectionManager
    RabbitMessagePublisher rabbitMessagePublisher
    ApplicationEventPublisher applicationEventPublisher

    def setup() {
        persistenceInterceptor = Mock(PersistenceContextInterceptor)
        connectionManager = Mock(ConnectionManager)
        rabbitMessagePublisher = Mock(RabbitMessagePublisher)
        applicationEventPublisher = Mock(ApplicationEventPublisher)

        messageConverterManager = new MessageConverterManagerImpl()
        messageConverterManager.register(new JsonMessageConverter())
        messageConverterManager.register(new LongMessageConverter())
        messageConverterManager.register(new StringMessageConverter())
    }

    AbstractConsumerContext createConsumerContext(Object consumer, ConsumerConfiguration consumerConfiguration, Object result) {
        return new MockConsumerContext(
            consumer,
            consumerConfiguration,
            result,
            connectionManager,
            persistenceInterceptor,
            rabbitMessagePublisher,
            applicationEventPublisher
        )
    }

    def 'Validate retrieving the context ID'() {
        when:
        ConsumerContext context = createConsumerContext(new Object(), null, null)

        then:
        context.getId() == Object.class.getName()
    }

    def 'Validate retrieving the connection name'() {
        setup:
        ConsumerConfiguration consumerConfiguration = new ConsumerConfigurationImpl()
        consumerConfiguration.connection = 'foobar'

        when:
        ConsumerContext context = createConsumerContext(new Object(), consumerConfiguration, null)

        then:
        context.getConnectionName() == 'foobar'
    }

    def 'Verify that the proper consumer callbacks are invoked for a successful message'() {
        setup:
        ConsumerConfiguration consumerConfiguration = new ConsumerConfigurationImpl()
        consumerConfiguration.queue = 'test-queue'

        MessageConsumerEventHandler consumer = Mock(MessageConsumerEventHandler)

        ConsumerContext consumerContext = createConsumerContext(consumer, consumerConfiguration, null)

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
        0 * consumer.onFailure((MessageContext) _, (Throwable) _)
    }

    def 'Verify that the proper consumer callbacks are invoked for an unsuccessful message'() {
        setup:
        ConsumerConfiguration consumerConfiguration = new ConsumerConfigurationImpl()
        consumerConfiguration.queue = 'test-queue'

        MessageConsumerEventHandler consumer = Mock(MessageConsumerEventHandler)

        ConsumerContext consumerContext = createConsumerContext(consumer, consumerConfiguration, new RuntimeException())

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
        0 * consumer.onSuccess((MessageContext) _)
        1 * consumer.onComplete(messageContext)
        1 * consumer.onFailure((MessageContext) messageContext, (Throwable) _)
    }

    def 'Start a basic consumer'() {
        setup:
        ConnectionContext connectionContext = Mock(ConnectionContext)
        connectionContext.getRunningState() >> RunningState.RUNNING
        connectionContext.createChannel() >> {
            return Mock(Channel)
        }

        connectionManager.getContext(_) >> connectionContext

        ConsumerConfiguration configuration = new ConsumerConfigurationImpl()
        configuration.queue = 'test-queue'
        configuration.consumers = 5

        ConsumerContext context = createConsumerContext(new Object(), configuration, null)

        when:
        context.start()

        then:
        context.getStatusReport().numConfigured == 5
    }

    def 'If the consumer has already been started and tried to start again, throw an IllegalStateException'() {
        setup:
        ConnectionContext connectionContext = Mock(ConnectionContext)
        connectionContext.createChannel() >> {
            return Mock(Channel)
        }
        connectionContext.getRunningState() >> RunningState.RUNNING
        connectionManager.getContext(_) >> connectionContext

        ConsumerConfiguration consumerConfiguration = new ConsumerConfigurationImpl(queue: 'foobar')

        AbstractConsumerContext context = createConsumerContext(new Object(), consumerConfiguration, null)

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
        channel.queueDeclare() >> { new DeclareOk('test-queue', 0, 0) }

        ConnectionContext connectionContext = Mock(ConnectionContext)
        connectionContext.getRunningState() >> RunningState.RUNNING
        connectionContext.createChannel() >> channel

        connectionManager.getContext(_) >> connectionContext

        ConsumerContext context = createConsumerContext(new Object(), configuration, null)

        when:
        context.start()

        then:
        context.getStatusReport().numConfigured == 1
        1 * channel.basicConsume('test-queue', (Boolean) _, (Consumer) _)
    }

    def 'If the persistence interceptor is present, validate its interactions'() {
        setup:
        ConsumerConfiguration configuration = new ConsumerConfigurationImpl()
        configuration.queue = 'test-queue'

        ConsumerContext consumerContext = createConsumerContext(new Object(), configuration, null)

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

        BasicProperties properties = Mock(BasicProperties)
        properties.getReplyTo() >> 'test-queue'
        properties.getContentType() >> ''

        MessageContext messageContext = new MessageContext(
            channel: Mock(Channel),
            envelope: Mock(Envelope),
            properties: properties,
            body: 'asdf'.getBytes()
        )

        ConsumerContext consumerContext = createConsumerContext(new Object(), configuration, 'foobar')

        rabbitMessagePublisher.send((RabbitMessageProperties) _) >> { throw new StackOverflowError() }

        consumerContext.log = log

        when:
        consumerContext.deliverMessage(messageContext)

        then:
        notThrown StackOverflowError
        1 * log.error('unexpected exception class java.lang.StackOverflowError encountered while responding from an RPC call with handler java.lang.Object', _)
    }

    def 'Ensure that consumer context start events are published in the correct order'() {
        setup:
        Channel channel = Mock(Channel)

        ConnectionContext connectionContext = Mock(ConnectionContext)
        connectionContext.getRunningState() >> RunningState.RUNNING
        connectionContext.createChannel() >> channel

        connectionManager.getContext(_) >> connectionContext

        ConsumerConfiguration configuration = Mock(ConsumerConfiguration)
        configuration.isValid() >> true
        configuration.getQueue() >> 'foobar'
        configuration.getConsumers() >> 1

        AbstractConsumerContext consumerContext = createConsumerContext(new Object(), configuration, null)

        when:
        consumerContext.start()

        then:
        1 * applicationEventPublisher.publishEvent({ it instanceof ConsumerContextStartingEvent })
        0 * applicationEventPublisher.publishEvent({ it instanceof ConsumerContextStartedEvent })
        0 * channel.basicConsume(_, _, _)

        then:
        0 * applicationEventPublisher.publishEvent({ it instanceof ConsumerContextStartedEvent })
        1 * channel.basicConsume(_, _, _)

        then:
        1 * applicationEventPublisher.publishEvent({ it instanceof ConsumerContextStartedEvent })
    }

    def 'Ensure that consumer context stop events are published in the correct order'() {
        setup:
        RabbitMessageHandler rabbitMessageHandler = Mock(RabbitMessageHandler)
        rabbitMessageHandler.getRunningState() >> RunningState.RUNNING

        ConsumerConfiguration configuration = Mock(ConsumerConfiguration)
        configuration.isValid() >> true
        configuration.getQueue() >> 'foobar'
        configuration.getConsumers() >> 1

        AbstractConsumerContext consumerContext = createConsumerContext(new Object(), configuration, null)

        consumerContext.consumers = [rabbitMessageHandler]

        when:
        consumerContext.stop()

        then:
        1 * applicationEventPublisher.publishEvent({ it instanceof ConsumerContextStoppingEvent })
        0 * applicationEventPublisher.publishEvent({ it instanceof ConsumerContextStoppedEvent })
        0 * rabbitMessageHandler.stop()

        then:
        0 * applicationEventPublisher.publishEvent({ it instanceof ConsumerContextStoppedEvent })
        1 * rabbitMessageHandler.stop()

        then:
        1 * applicationEventPublisher.publishEvent({ it instanceof ConsumerContextStoppedEvent })
    }

    def 'When a no message handler and message converter combination are available to handle an incoming message, and \
            the consumer implements UnsupportedMessageHandler, handleUnsupportedMessage is called'() {
        setup:
        UnsupportedMessageHandler unsupportedMessageHandler = Mock(UnsupportedMessageHandler)

        BasicProperties basicProperties = Mock(BasicProperties)
        Envelope envelope = Mock(Envelope)
        Channel channel = Mock(Channel)

        MessageContext messageContext = Mock(MessageContext)
        messageContext.getBody() >> 'foobar'.bytes
        messageContext.getProperties() >> basicProperties
        messageContext.getEnvelope() >> envelope
        messageContext.getChannel() >> channel

        AbstractConsumerContext context = createConsumerContext(unsupportedMessageHandler, new ConsumerConfigurationImpl(queue: 'foobar'), new UnsupportedMessageException())

        when:
        context.deliverMessage(messageContext)

        then:
        1 * unsupportedMessageHandler.handleUnsupportedMessage(_)
    }
}

@InheritConstructors
class MockConsumerContext extends AbstractConsumerContext {
    final Object consumer
    final ConsumerConfiguration consumerConfiguration
    final Object result

    /**
     * Constructor.
     *
     * @param connectionManager
     * @param persistenceInterceptor
     * @param rabbitMessagePublisher @param applicationEventPublisher
     */
    MockConsumerContext(Object consumer, ConsumerConfiguration consumerConfiguration, Object result, ConnectionManager connectionManager, PersistenceContextInterceptor persistenceInterceptor, RabbitMessagePublisher rabbitMessagePublisher, ApplicationEventPublisher applicationEventPublisher) {
        super(connectionManager, persistenceInterceptor, rabbitMessagePublisher, applicationEventPublisher)

        this.consumer = consumer
        this.consumerConfiguration = consumerConfiguration
        this.result = result
    }

    @Override
    protected Object process(MessageContext messageContext) {
        if (result instanceof Throwable) {
            throw result
        }
        return result
    }
}