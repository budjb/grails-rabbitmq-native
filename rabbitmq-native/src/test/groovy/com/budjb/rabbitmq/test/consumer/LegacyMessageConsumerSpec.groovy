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

import com.budjb.rabbitmq.consumer.LegacyMessageConsumer
import com.budjb.rabbitmq.consumer.MessageContext
import com.budjb.rabbitmq.converter.*
import com.budjb.rabbitmq.exception.MissingConfigurationException
import com.budjb.rabbitmq.exception.NoMessageHandlersDefinedException
import com.budjb.rabbitmq.exception.UnsupportedMessageException
import com.budjb.rabbitmq.test.support.*
import com.rabbitmq.client.BasicProperties
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import grails.config.Config
import groovy.json.JsonOutput
import org.grails.config.PropertySourcesConfig
import spock.lang.Specification
import spock.lang.Unroll

class LegacyMessageConsumerSpec extends Specification {
    MessageConverterManager messageConverterManager

    def setup() {
        messageConverterManager = new MessageConverterManagerImpl()
        messageConverterManager.register(new JsonMessageConverter())
        messageConverterManager.register(new LongMessageConverter())
        messageConverterManager.register(new StringMessageConverter())
    }

    def 'If a consumer has a configuration defined in the application config, it is loaded correctly'() {
        setup:
        Config config = new PropertySourcesConfig([
            rabbitmq: [
                consumers: [
                    'UnitTestConsumer': [
                        queue    : 'test-queue',
                        consumers: 10
                    ]
                ]
            ]
        ])

        LegacyMessageConsumer consumer = new LegacyMessageConsumer(new UnitTestConsumer(), config, messageConverterManager)

        expect:
        consumer.id == 'com.budjb.rabbitmq.test.support.UnitTestConsumer'
        consumer.configuration.queue == 'test-queue'
        consumer.configuration.consumers == 10
    }

    def 'If a consumer has a configuration defined within the object, it is loaded correctly'() {
        setup:
        LegacyMessageConsumer consumer = new LegacyMessageConsumer(new UnitTestConsumer(), new PropertySourcesConfig(), messageConverterManager)

        expect:
        consumer.id == 'com.budjb.rabbitmq.test.support.UnitTestConsumer'
        consumer.configuration.queue == 'test-queue'
        consumer.configuration.consumers == 5
    }

    def 'If a consumer has no configuration defined, a MissingConfigurationException is thrown'() {
        when:
        new LegacyMessageConsumer(new MissingConfigurationConsumer(), new PropertySourcesConfig(), messageConverterManager)

        then:
        thrown MissingConfigurationException
    }

    def 'If a consumer has no message handlers defined, a NoMessageHandlersDefinedException is thrown'() {
        when:
        new LegacyMessageConsumer(new MissingHandlersConsumer(), new PropertySourcesConfig(), messageConverterManager)

        then:
        thrown NoMessageHandlersDefinedException
    }

    @Unroll
    def 'When a #type type is given to the MultipleMessageConsumer, the proper handler is called'() {
        setup:
        MultipleHandlersConsumer wrapped = new MultipleHandlersConsumer()

        LegacyMessageConsumer consumer = new LegacyMessageConsumer(wrapped, new PropertySourcesConfig(), messageConverterManager)

        MessageContext messageContext = new MessageContext()
        messageContext.body = body
        messageContext.channel = Mock(Channel)
        messageContext.envelope = Mock(Envelope)
        messageContext.consumerTag = 'foobar'
        messageContext.properties = Mock(BasicProperties)

        when:
        consumer.process(messageContext)

        then:
        wrapped.handler == handler

        where:
        type      | body                                  | handler
        'map'     | JsonOutput.toJson([foo: 'bar']).bytes | MultipleHandlersConsumer.Handler.MAP
        'integer' | 1234.toString().bytes                 | MultipleHandlersConsumer.Handler.INTEGER
        'b[]'     | [1, 2, 3] as byte[]                   | MultipleHandlersConsumer.Handler.BYTE
    }

    def 'When a handler does not exist for a given body type, an UnsupportedMessageException is thrown'() {
        setup:
        LegacyMessageConsumer messageConsumer = new LegacyMessageConsumer(new IntegerMessageConsumer(), new PropertySourcesConfig(), messageConverterManager)

        MessageContext messageContext = new MessageContext()
        messageContext.body = 'foobar'.bytes
        messageContext.channel = Mock(Channel)
        messageContext.envelope = Mock(Envelope)
        messageContext.consumerTag = 'foobar'
        messageContext.properties = Mock(BasicProperties)

        when:
        messageConsumer.process(messageContext)

        then:
        thrown UnsupportedMessageException
    }

    def 'When no message handler is available to handle an incoming message, and the consumer implements UnsupportedMessageHandler, the handler is called'() {
        setup:
        TestUnsupportedMessageConsumer testUnsupportedMessageConsumer = new TestUnsupportedMessageConsumer()
        LegacyMessageConsumer consumer = new LegacyMessageConsumer(testUnsupportedMessageConsumer, new PropertySourcesConfig(), messageConverterManager)
        MessageContext messageContext = Mock(MessageContext)

        expect:
        !testUnsupportedMessageConsumer.unsupportedCalled

        when:
        consumer.handleUnsupportedMessage(messageContext)

        then:
        testUnsupportedMessageConsumer.unsupportedCalled
    }

    def 'When no message handler is available to handle an incoming message, and the consumer does NOT implement UnsupportedMessageHandler, an UnsupportedMessageException is thrown'() {
        setup:
        IntegerMessageConsumer integerMessageConsumer = new IntegerMessageConsumer()
        LegacyMessageConsumer consumer = new LegacyMessageConsumer(integerMessageConsumer, new PropertySourcesConfig(), messageConverterManager)
        MessageContext messageContext = Mock(MessageContext)

        when:
        consumer.handleUnsupportedMessage(messageContext)

        then:
        thrown UnsupportedMessageException
    }

    def 'Validate that the handlers of wrapped consumers are called'() {
        setup:
        WrappedMessageConsumer wrappedMessageConsumer = new WrappedMessageConsumer()

        LegacyMessageConsumer messageConsumer = new LegacyMessageConsumer(wrappedMessageConsumer, new PropertySourcesConfig(), messageConverterManager)
        MessageContext messageContext = Mock(MessageContext)

        when: 'the wrapper\'s onReceive is called'
        messageConsumer.onReceive(messageContext)

        then: 'the wrapped consumer\'s onReceive is called'
        wrappedMessageConsumer.callback == WrappedMessageConsumer.Callback.ON_RECEIVE

        when: 'the wrapper\'s onSuccess is called'
        messageConsumer.onSuccess(messageContext)

        then: 'the wrapped consumer\'s onSuccess is called'
        wrappedMessageConsumer.callback == WrappedMessageConsumer.Callback.ON_SUCCESS

        when: 'the wrapper\'s onFailure is called'
        messageConsumer.onFailure(messageContext, new Exception())

        then: 'the wrapped consumer\'s onFailure is called'
        wrappedMessageConsumer.callback == WrappedMessageConsumer.Callback.ON_FAILURE

        when: 'the wrapper\'s onComplete is called'
        messageConsumer.onComplete(messageContext)

        then: 'the wrapped consumer\'s onComplete is called'
        wrappedMessageConsumer.callback == WrappedMessageConsumer.Callback.ON_COMPLETE
    }
}
