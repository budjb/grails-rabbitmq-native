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
package com.budjb.rabbitmq.test.connection

import com.budjb.rabbitmq.connection.ConnectionConfiguration
import com.budjb.rabbitmq.connection.ConnectionConfigurationImpl
import com.budjb.rabbitmq.connection.ConnectionContext
import com.budjb.rabbitmq.connection.ConnectionContextImpl
import com.budjb.rabbitmq.event.ConnectionContextStartedEvent
import com.budjb.rabbitmq.event.ConnectionContextStartingEvent
import com.budjb.rabbitmq.event.ConnectionContextStoppedEvent
import com.budjb.rabbitmq.event.ConnectionContextStoppingEvent
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import org.slf4j.Logger
import org.springframework.context.ApplicationEventPublisher
import spock.lang.Specification

import java.util.concurrent.ExecutorService

class ConnectionContextImplSpec extends Specification {
    ApplicationEventPublisher applicationEventPublisher

    def setup() {
        applicationEventPublisher = Mock(ApplicationEventPublisher)
    }

    def 'If a connection context has an invalid configuration, the context should not start'() {
        setup:
        ConnectionConfiguration configuration = Mock(ConnectionConfiguration)
        configuration.getName() >> "test-connection"
        ConnectionFactory factory = Mock(ConnectionFactory)

        Logger log = Mock(Logger)

        ConnectionContextImpl context = new ConnectionContextImpl(configuration, applicationEventPublisher)
        context.log = log
        context.connectionFactory = factory

        when:
        context.start()

        then:
        1 * log.error("unable to start connection 'test-connection' because its configuration is invalid")
        0 * factory._
    }

    def 'If a null connection configuration is passed to the constructor, a null pointer exception is thrown'() {
        when:
        new ConnectionContextImpl(null, applicationEventPublisher)

        then:
        thrown NullPointerException

    }

    def 'Verify start() interactions'() {
        setup:
        ConnectionConfigurationImpl connectionConfiguration = new ConnectionConfigurationImpl([
            'host'    : 'localhost',
            'username': 'guest',
            'password': 'guest'
        ])
        ConnectionFactory connectionFactory = Mock(ConnectionFactory)
        ConnectionContext connectionContext = new ConnectionContextImpl(connectionConfiguration, applicationEventPublisher)
        connectionContext.setConnectionFactory(connectionFactory)

        when:
        connectionContext.start()

        then:
        1 * connectionFactory.newConnection((ExecutorService) _)
        1 * connectionFactory.setHost('localhost')
        1 * connectionFactory.setUsername('guest')
        1 * connectionFactory.setPassword('guest')
    }

    def 'Validate stop() interactions'() {
        setup:
        ConnectionConfigurationImpl connectionConfiguration = new ConnectionConfigurationImpl([
            'host'    : 'localhost',
            'username': 'guest',
            'password': 'guest'
        ])
        Connection connection = Mock(Connection)
        connection.isOpen() >> true

        ConnectionContext connectionContext = new ConnectionContextImpl(connectionConfiguration, applicationEventPublisher)
        connectionContext.connection = connection

        when:
        connectionContext.stop()

        then:
        1 * connection.close()

        when:
        connectionContext.stop()

        then:
        0 * connection.close()
    }

    def 'Verify getConnection() behavior during started/stopped states'() {
        ConnectionConfigurationImpl connectionConfiguration = new ConnectionConfigurationImpl([
            'host'    : 'localhost',
            'username': 'guest',
            'password': 'guest',
            'ssl'     : true,
            'threads' : 5
        ])
        ConnectionFactory connectionFactory = Mock(ConnectionFactory)
        ConnectionContext connectionContext = new ConnectionContextImpl(connectionConfiguration, applicationEventPublisher)
        connectionContext.setConnectionFactory(connectionFactory)

        when:
        connectionContext.getConnection()

        then:
        thrown IllegalStateException

        when:
        connectionContext.connection = Mock(Connection)
        connectionContext.getConnection()

        then:
        notThrown Throwable
    }

    def 'If creating a channel is attempted with no live connection, an IllegalStateException is thrown'() {
        setup:
        ConnectionConfiguration configuration = Mock(ConnectionConfiguration)
        ConnectionContextImpl context = new ConnectionContextImpl(configuration, applicationEventPublisher)

        when:
        context.createChannel()

        then:
        thrown IllegalStateException
    }

    def 'Ensure that connection context start events are published in the correct order'() {
        setup:
        ConnectionConfiguration configuration = Mock(ConnectionConfiguration)
        configuration.isValid() >> true

        ConnectionFactory connectionFactory = Mock(ConnectionFactory)

        ConnectionContext connectionContext = new ConnectionContextImpl(configuration, applicationEventPublisher)
        connectionContext.setConnectionFactory(connectionFactory)

        when:
        connectionContext.start()

        then:
        1 * applicationEventPublisher.publishEvent({ it instanceof ConnectionContextStartingEvent })
        0 * applicationEventPublisher.publishEvent({ it instanceof ConnectionContextStartedEvent })
        0 * connectionFactory.newConnection((ExecutorService) _)

        then:
        0 * applicationEventPublisher.publishEvent({ it instanceof ConnectionContextStartedEvent })
        1 * connectionFactory.newConnection((ExecutorService) _)

        then:
        1 * applicationEventPublisher.publishEvent({ it instanceof ConnectionContextStartedEvent })
    }

    def 'Ensure that connection context stop events are published in the correct order'() {
        setup:
        ConnectionConfiguration configuration = Mock(ConnectionConfiguration)

        Connection connection = Mock(Connection)
        connection.isOpen() >> true

        ConnectionContext connectionContext = new ConnectionContextImpl(configuration, applicationEventPublisher)
        connectionContext.setConnection(connection)

        when:
        connectionContext.stop()

        then:
        1 * applicationEventPublisher.publishEvent({ it instanceof ConnectionContextStoppingEvent })
        0 * applicationEventPublisher.publishEvent({ it instanceof ConnectionContextStoppedEvent })
        0 * connection.close()

        then:
        0 * applicationEventPublisher.publishEvent({ it instanceof ConnectionContextStoppedEvent })
        1 * connection.close()

        then:
        1 * applicationEventPublisher.publishEvent({ it instanceof ConnectionContextStoppedEvent })
    }
}
