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
import com.budjb.rabbitmq.consumer.ConsumerContext
import com.budjb.rabbitmq.consumer.ConsumerContextImpl
import com.budjb.rabbitmq.consumer.ConsumerManagerImpl
import com.budjb.rabbitmq.converter.MessageConverterManager
import com.budjb.rabbitmq.exception.ContextNotFoundException
import com.budjb.rabbitmq.exception.MissingConfigurationException
import com.budjb.rabbitmq.publisher.RabbitMessagePublisher
import com.budjb.rabbitmq.test.MissingConfigurationConsumer
import com.budjb.rabbitmq.test.UnitTestConsumer
import org.apache.log4j.Logger
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClass
import org.springframework.context.ApplicationContext
import spock.lang.Specification

class ConsumerManagerImplSpec extends Specification {
    GrailsApplication grailsApplication
    Object persistenceInterceptor
    MessageConverterManager messageConverterManager
    RabbitMessagePublisher rabbitMessagePublisher
    ConnectionManager connectionManager
    ConsumerManagerImpl consumerManager
    ApplicationContext applicationContext

    def setup() {
        grailsApplication = Mock(GrailsApplication)
        persistenceInterceptor = null
        connectionManager = Mock(ConnectionManager)
        messageConverterManager = Mock(MessageConverterManager)
        rabbitMessagePublisher = Mock(RabbitMessagePublisher)
        applicationContext = Mock(ApplicationContext)

        consumerManager = new ConsumerManagerImpl()
        consumerManager.grailsApplication = grailsApplication
        consumerManager.persistenceInterceptor = persistenceInterceptor
        consumerManager.messageConverterManager = messageConverterManager
        consumerManager.rabbitMessagePublisher = rabbitMessagePublisher
        consumerManager.connectionManager = connectionManager
        consumerManager.applicationContext = applicationContext
    }

    def 'Ensure proper objects are injected into new contexts'() {
        setup:
        UnitTestConsumer consumer = new UnitTestConsumer()

        when:
        ConsumerContextImpl context = consumerManager.createContext(consumer)

        then:
        context.consumer == consumer
        context.configuration instanceof ConsumerConfiguration
        context.persistenceInterceptor == persistenceInterceptor
        context.messageConverterManager == messageConverterManager
        context.rabbitMessagePublisher == rabbitMessagePublisher
        context.connectionManager == connectionManager
    }

    def 'Test load/registering of consumer artefacts'() {
        setup:
        GrailsClass artefact1 = Mock(GrailsClass)
        artefact1.getPropertyName() >> 'consumer1'

        GrailsClass artefact2 = Mock(GrailsClass)
        artefact2.getPropertyName() >> 'consumer2'

        Consumer1 consumer1 = new Consumer1()
        Consumer2 consumer2 = new Consumer2()

        applicationContext.getBean('consumer1') >> consumer1
        applicationContext.getBean('consumer2') >> consumer2

        grailsApplication.getArtefacts('MessageConsumer') >> [artefact1, artefact2]

        when:
        consumerManager.load()

        then:
        consumerManager.consumers.size() == 2
    }

    def 'If a consumer is registered, validate it can be retrieved by its name'() {
        setup:
        ConsumerContext consumerContext = Mock(ConsumerContext)
        consumerContext.getId() >> 'TestConsumer'
        consumerManager.register(consumerContext)

        expect:
        consumerManager.getContext('TestConsumer') != null
    }

    def 'If a consumer is requested by name but it does not exist, a ContextNotFoundException should be thrown'() {
        when:
        consumerManager.getContext('TestConsumer')

        then:
        thrown ContextNotFoundException
    }

    def 'If a consumer is un-registered, ensure it is stopped first'() {
        setup:
        ConsumerContext consumerContext = Mock(ConsumerContext)
        consumerContext.getId() >> 'TestConsumer'
        consumerManager.register(consumerContext)

        when:
        consumerManager.unregister(consumerContext)

        then:
        1 * consumerContext.stop()
        consumerManager.consumers.size() == 0
    }

    def 'If the manager is reset, all consumers should be stopped and un-registered'() {
        setup:
        ConsumerContext consumerContext1 = Mock(ConsumerContext)
        consumerContext1.getId() >> "Consumer1"

        ConsumerContext consumerContext2 = Mock(ConsumerContext)
        consumerContext2.getId() >> "Consumer2"

        consumerManager.register(consumerContext1)
        consumerManager.register(consumerContext2)

        when:
        consumerManager.reset()

        then:
        1 * consumerContext1.stop()
        1 * consumerContext2.stop()
        consumerManager.consumers.size() == 0
    }

    def 'If the manager is started, all consumer contexts should also be started'() {
        setup:
        ConsumerContext consumerContext1 = Mock(ConsumerContext)
        consumerContext1.getId() >> "Consumer1"

        ConsumerContext consumerContext2 = Mock(ConsumerContext)
        consumerContext2.getId() >> "Consumer2"

        consumerManager.register(consumerContext1)
        consumerManager.register(consumerContext2)

        when:
        consumerManager.start()

        then:
        1 * consumerContext1.start()
        1 * consumerContext2.start()
    }

    def 'If a consumer is started by name, the correct consumer is started'() {
        setup:
        ConsumerContext consumerContext1 = Mock(ConsumerContext)
        consumerContext1.getId() >> "Consumer1"

        ConsumerContext consumerContext2 = Mock(ConsumerContext)
        consumerContext2.getId() >> "Consumer2"

        consumerManager.register(consumerContext1)
        consumerManager.register(consumerContext2)

        when:
        consumerManager.start('Consumer1')

        then:
        1 * consumerContext1.start()
        0 * consumerContext2.start()
    }

    def 'If the manager is stopped, all consumer contexts should also be stopped'() {
        setup:
        ConsumerContext consumerContext1 = Mock(ConsumerContext)
        consumerContext1.getId() >> "Consumer1"

        ConsumerContext consumerContext2 = Mock(ConsumerContext)
        consumerContext2.getId() >> "Consumer2"

        consumerManager.register(consumerContext1)
        consumerManager.register(consumerContext2)

        when:
        consumerManager.stop()

        then:
        1 * consumerContext1.stop()
        1 * consumerContext2.stop()
    }

    def 'If a consumer is stopped by name, the correct consumer is stopped'() {
        setup:
        ConsumerContext consumerContext1 = Mock(ConsumerContext)
        consumerContext1.getId() >> "Consumer1"

        ConsumerContext consumerContext2 = Mock(ConsumerContext)
        consumerContext2.getId() >> "Consumer2"

        consumerManager.register(consumerContext1)
        consumerManager.register(consumerContext2)

        when:
        consumerManager.stop('Consumer1')

        then:
        1 * consumerContext1.stop()
        0 * consumerContext2.stop()
        consumerManager.consumers.size() == 2
    }

    def 'If a consumer is registered with the same name as another consumer, the old one is stopped and un-registered'() {
        setup:
        ConsumerContext consumerContext1 = Mock(ConsumerContext)
        consumerContext1.getId() >> "Consumer1"

        ConsumerContext consumerContext2 = Mock(ConsumerContext)
        consumerContext2.getId() >> "Consumer1"

        consumerManager.register(consumerContext1)

        when:
        consumerManager.register(consumerContext2)

        then:
        1 * consumerContext1.stop()
        consumerManager.consumers.size() == 1

        expect:
        consumerManager.getContext('Consumer1') == consumerContext2
    }

    def 'If a consumer has a configuration defined in the application config, it is loaded correctly'() {
        setup:
        grailsApplication.getConfig() >> new ConfigObject([
            rabbitmq: [
                consumers: [
                    'MissingConfigurationConsumer': [
                        queue: 'test-queue',
                        consumers: 10
                    ]
                ]
            ]
        ])
        MissingConfigurationConsumer consumer = new MissingConfigurationConsumer()

        when:
        ConsumerContext consumerContext = consumerManager.createContext(consumer)

        then:
        consumerContext.id == 'MissingConfigurationConsumer'
        consumerContext.configuration.queue == 'test-queue'
        consumerContext.configuration.consumers == 10
    }

    def 'If a consumer has a configuration defined within the object, it is loaded correctly'() {
        setup:
        UnitTestConsumer consumer = new UnitTestConsumer()

        when:
        ConsumerContext consumerContext = consumerManager.createContext(consumer)

        then:
        consumerContext.id == 'UnitTestConsumer'
        consumerContext.configuration.queue == 'test-queue'
        consumerContext.configuration.consumers == 5
    }

    def 'If a consumer has no configuration defined, a MissingConfigurationException is thrown'() {
        setup:
        grailsApplication.getConfig() >> new ConfigObject()
        MissingConfigurationConsumer consumer = new MissingConfigurationConsumer()

        when:
        ConsumerContext consumerContext = consumerManager.createContext(consumer)

        then:
        thrown MissingConfigurationException
    }

    def 'If a consumer is loaded but is missing its configuration, a warning is logged but no exception is thrown'() {
        setup:
        grailsApplication.getConfig() >> new ConfigObject()

        GrailsClass artefact1 = Mock(GrailsClass)
        artefact1.getPropertyName() >> 'unitTestConsumer'
        artefact1.getShortName() >> 'UnitTestConsumer'

        GrailsClass artefact2 = Mock(GrailsClass)
        artefact2.getPropertyName() >> 'missingConfigurationConsumer'
        artefact2.getShortName() >> 'MissingConfigurationConsumer'

        UnitTestConsumer consumer1 = new UnitTestConsumer()
        MissingConfigurationConsumer consumer2 = new MissingConfigurationConsumer()

        applicationContext.getBean('unitTestConsumer') >> consumer1
        applicationContext.getBean('missingConfigurationConsumer') >> consumer2

        grailsApplication.getArtefacts('MessageConsumer') >> [artefact1, artefact2]

        Logger log = Mock(Logger)
        consumerManager.log = log

        when:
        consumerManager.load()

        then:
        1 * log.warn("not loading consumer 'MissingConfigurationConsumer' because its configuration is missing")
        consumerManager.consumers.size() == 1
    }

    def 'If all consumers are started while some of those consumers are already started, the IllegalStateException should be swallowed'() {
        setup:
        ConsumerContext consumer1 = Mock(ConsumerContext)
        ConsumerContext consumer2 = Mock(ConsumerContext)

        consumerManager.consumers = [consumer1, consumer2]

        when:
        consumerManager.start(consumer1)

        then:
        1 * consumer1.start()
        0 * consumer2.start()

        when:
        consumerManager.start()

        then:
        1 * consumer1.start() >> { throw new IllegalStateException('already started bro') }
        1 * consumer2.start()
        notThrown IllegalStateException
    }

    def 'When starting consumers based on their connection context link, only the correct consumers are started'() {
        setup:
        ConsumerContext consumer1 = Mock(ConsumerContext)
        ConsumerContext consumer2 = Mock(ConsumerContext)

        consumer1.getConnectionName() >> 'connection1'
        consumer2.getConnectionName() >> 'connection2'

        ConnectionContext connectionContext = Mock(ConnectionContext)
        connectionContext.getId() >> 'connection2'

        consumerManager.consumers = [consumer1, consumer2]

        when:
        consumerManager.start(connectionContext)

        then:
        0 * consumer1.start()
        1 * consumer2.start()
    }

    def 'IllegalStateException should be swallowed when starting consumers based on their connection context link and some are already started'() {
        setup:
        ConsumerContext consumer1 = Mock(ConsumerContext)
        ConsumerContext consumer2 = Mock(ConsumerContext)

        consumer1.getConnectionName() >> 'connection1'
        consumer2.getConnectionName() >> 'connection2'

        ConnectionContext connectionContext = Mock(ConnectionContext)
        connectionContext.getId() >> 'connection2'

        consumerManager.consumers = [consumer1, consumer2]

        when:
        consumerManager.start(connectionContext)

        then:
        0 * consumer1.start()
        1 * consumer2.start()

        when:
        consumerManager.start(connectionContext)

        then:
        0 * consumer1.start()
        1 * consumer2.start() >> { throw new IllegalStateException('already started bro') }
        notThrown IllegalStateException
    }

    def 'When stopping consumers based on their connection context link, only the correct consumers are stopped'() {
        setup:
        ConsumerContext consumer1 = Mock(ConsumerContext)
        ConsumerContext consumer2 = Mock(ConsumerContext)

        consumer1.getConnectionName() >> 'connection1'
        consumer2.getConnectionName() >> 'connection2'

        ConnectionContext connectionContext = Mock(ConnectionContext)
        connectionContext.getId() >> 'connection2'

        consumerManager.consumers = [consumer1, consumer2]

        when:
        consumerManager.stop(connectionContext)

        then:
        0 * consumer1.stop()
        1 * consumer2.stop()
    }

    class Consumer1 {
        static rabbitConfig = [
            'queue': 'test-queue-1'
        ]

        def handleMessage(def body, def messageConext) {

        }
    }

    class Consumer2 {
        static rabbitConfig = [
            'queue': 'test-queue-2'
        ]

        def handleMessage(def body, def messageContext) {

        }
    }
}
