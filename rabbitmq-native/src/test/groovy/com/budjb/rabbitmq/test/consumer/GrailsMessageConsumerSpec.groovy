/*
 * Copyright 2017 Bud Byrd
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

import com.budjb.rabbitmq.consumer.MessageConsumer
import com.budjb.rabbitmq.consumer.MessageContext
import com.budjb.rabbitmq.converter.*
import com.budjb.rabbitmq.exception.MissingConfigurationException
import com.budjb.rabbitmq.exception.NoMessageHandlersDefinedException
import com.budjb.rabbitmq.test.support.*
import com.rabbitmq.client.BasicProperties
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import grails.config.Config
import grails.core.GrailsApplication
import groovy.json.JsonOutput
import org.grails.config.PropertySourcesConfig
import spock.lang.Specification
import spock.lang.Unroll

class GrailsMessageConsumerSpec extends Specification {
    MessageConverterManager messageConverterManager
    GrailsApplication grailsApplication

    def setup() {
        messageConverterManager = new MessageConverterManagerImpl()
        messageConverterManager.register(new MapMessageConverter())
        messageConverterManager.register(new IntegerMessageConverter())
        messageConverterManager.register(new StringMessageConverter())

        grailsApplication = Mock(GrailsApplication)
    }

    def 'If a consumer has a configuration defined in the application config, it is loaded correctly'() {
        setup:
        Config config = new PropertySourcesConfig()
        config.putAll([
            rabbitmq: [
                consumers: [
                    'UnitTestConsumer': [
                        queue    : 'test-queue',
                        consumers: 10
                    ]
                ]
            ]
        ])

        grailsApplication.getConfig() >> config

        UnitTestConsumer consumer = new UnitTestConsumer()
        consumer.grailsApplication = grailsApplication
        consumer.init()

        expect:
        consumer.id == 'com.budjb.rabbitmq.test.support.UnitTestConsumer'
        consumer.configuration.queue == 'test-queue'
        consumer.configuration.consumers == 10
    }

    def 'If a consumer has a configuration defined within the object, it is loaded correctly'() {
        setup:
        grailsApplication.getConfig() >> new PropertySourcesConfig()

        UnitTestConsumer consumer = new UnitTestConsumer()
        consumer.grailsApplication = grailsApplication
        consumer.init()

        expect:
        consumer.id == 'com.budjb.rabbitmq.test.support.UnitTestConsumer'
        consumer.configuration.queue == 'test-queue'
        consumer.configuration.consumers == 5
    }

    def 'If a consumer has no configuration defined, a MissingConfigurationException is thrown'() {
        setup:
        grailsApplication.getConfig() >> new PropertySourcesConfig()

        MissingConfigurationConsumer consumer = new MissingConfigurationConsumer()
        consumer.grailsApplication = grailsApplication

        when:
        consumer.init()

        then:
        thrown MissingConfigurationException
    }

    def 'If a consumer has no message handlers defined, a NoMessageHandlersDefinedException is thrown'() {
        setup:
        grailsApplication.getConfig() >> new PropertySourcesConfig()

        MessageConsumer messageConsumer = new MissingHandlersConsumer()
        messageConsumer.grailsApplication = grailsApplication

        when:
        messageConsumer.init()

        then:
        thrown NoMessageHandlersDefinedException
    }

    @Unroll
    def 'When a #type type is given to the MultipleMessageConsumer, the proper handler is called'() {
        setup:
        grailsApplication.getConfig() >> new PropertySourcesConfig()

        MultipleHandlersConsumer consumer = new MultipleHandlersConsumer()
        consumer.grailsApplication = grailsApplication
        consumer.messageConverterManager = messageConverterManager
        consumer.init()

        MessageContext messageContext = new MessageContext()
        messageContext.body = body
        messageContext.channel = Mock(Channel)
        messageContext.envelope = Mock(Envelope)
        messageContext.consumerTag = 'foobar'
        messageContext.properties = Mock(BasicProperties)

        when:
        consumer.process(messageContext)

        then:
        consumer.handler == handler

        where:
        type      | body                                  | handler
        'map'     | JsonOutput.toJson([foo: 'bar']).bytes | MultipleHandlersConsumer.Handler.MAP
        'integer' | 1234.toString().bytes                 | MultipleHandlersConsumer.Handler.INTEGER
        'b[]'     | [1, 2, 3] as byte[]                   | MultipleHandlersConsumer.Handler.BYTE
    }

    def 'When a handler does not exist for a given body type, an IllegalArgumentException is thrown'() {
        setup:
        MessageConsumer messageConsumer = new IntegerMessageConsumer()
        messageConsumer.messageConverterManager = messageConverterManager
        messageConsumer.init()

        MessageContext messageContext = new MessageContext()
        messageContext.body = 'foobar'.bytes
        messageContext.channel = Mock(Channel)
        messageContext.envelope = Mock(Envelope)
        messageContext.consumerTag = 'foobar'
        messageContext.properties = Mock(BasicProperties)

        when:
        messageConsumer.process(messageContext)

        then:
        thrown IllegalArgumentException
    }
}
