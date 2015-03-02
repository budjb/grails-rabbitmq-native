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
package com.budjb.rabbitmq.test.connection

import com.budjb.rabbitmq.connection.ConnectionConfiguration
import com.budjb.rabbitmq.connection.ConnectionConfigurationImpl
import com.rabbitmq.client.ConnectionFactory
import org.apache.log4j.Logger
import spock.lang.Specification

class ConnectionConfigurationImplSpec extends Specification {
    def 'Test default values when bad values are provided'() {
        setup:
        Map properties = [
            'host': 'test-host',
            'username': 'test-user',
            'password': 'test-password',
            'port': 'foobar' // this is an integer field and the value should not be assigned
        ]

        when:
        ConnectionConfiguration configuration = new ConnectionConfigurationImpl(properties)

        then:
        configuration.getPort() == 5672
    }

    def 'Test log output for missing required values'() {
        setup:
        Logger log = Mock(Logger)
        ConnectionConfiguration configuration = new ConnectionConfigurationImpl()
        configuration.log = log
        configuration.setPort(0)
        configuration.setVirtualHost('')
        configuration.setThreads(-1)

        when:
        configuration.isValid()

        then:
        1 * log.warn("RabbitMQ connection host configuration is missing")
        1 * log.warn("RabbitMQ connection username is missing")
        1 * log.warn("RabbitMQ connection password is missing")
        1 * log.warn("RabbitMQ connection virtualHost is missing")
        1 * log.warn("RabbitMQ connection port is missing")
        1 * log.warn("RabbitMQ connection threads must be greater than or equal to 0")
    }

    def 'Test default configuration options'() {
        setup:
        Map properties = [
            'host': 'test-host',
            'username': 'test-user',
            'password': 'test-password'
        ]

        when:
        ConnectionConfiguration connectionConfiguration = new ConnectionConfigurationImpl(properties)

        then:
        connectionConfiguration.getHost() == 'test-host'
        connectionConfiguration.getUsername() == 'test-user'
        connectionConfiguration.getPassword() == 'test-password'
        connectionConfiguration.getAutomaticReconnect() == true
        connectionConfiguration.getIsDefault() == false
        !connectionConfiguration.getName().isEmpty()
        connectionConfiguration.getPort() == ConnectionFactory.DEFAULT_AMQP_PORT
        connectionConfiguration.getRequestedHeartbeat() == ConnectionFactory.DEFAULT_HEARTBEAT
        connectionConfiguration.getSsl() == false
        connectionConfiguration.getThreads() == 0
        connectionConfiguration.getVirtualHost() == ConnectionFactory.DEFAULT_VHOST
    }

    def 'Missing required properties should throw an exception'() {
        setup:
        Logger log = Mock(Logger)
        ConnectionConfiguration configuration = new ConnectionConfigurationImpl([:])
        configuration.log = log

        when:
        boolean valid = configuration.isValid()

        then:
        valid == false

        1 * log.warn("RabbitMQ connection host configuration is missing")
        1 * log.warn("RabbitMQ connection username is missing")
        1 * log.warn("RabbitMQ connection password is missing")
        0 * log._
    }

    def 'Test non-default configuration options'() {
        setup:
        Map properties = [
            'host': 'test-host',
            'username': 'test-user',
            'password': 'test-password',
            'automaticReconnect': false,
            'isDefault': true,
            'name': 'test-connection-name',
            'port': 10000,
            'requestedHeartbeat': 1000,
            'ssl': true,
            'threads': 10,
            'virtualHost': 'test-virtual-host'
        ]

        when:
        ConnectionConfigurationImpl connectionConfiguration = new ConnectionConfigurationImpl(properties)

        then:
        connectionConfiguration.getHost() == 'test-host'
        connectionConfiguration.getUsername() == 'test-user'
        connectionConfiguration.getPassword() == 'test-password'
        connectionConfiguration.getAutomaticReconnect() == false
        connectionConfiguration.getIsDefault() == true
        connectionConfiguration.getName() == 'test-connection-name'
        connectionConfiguration.getPort() == 10000
        connectionConfiguration.getRequestedHeartbeat() == 1000
        connectionConfiguration.getSsl() == true
        connectionConfiguration.getThreads() == 10
        connectionConfiguration.getVirtualHost() == 'test-virtual-host'
    }

    def 'Test that setters work correctly'() {
        setup:
        ConnectionConfigurationImpl configuration = new ConnectionConfigurationImpl()

        when:
        configuration.setHost('test-host')
        configuration.setUsername('test-user')
        configuration.setPassword('test-password')
        configuration.setAutomaticReconnect(false)
        configuration.setIsDefault(true)
        configuration.setName('test-connection-name')
        configuration.setPort(10000)
        configuration.setRequestedHeartbeat(1000)
        configuration.setSsl(true)
        configuration.setThreads(10)
        configuration.setVirtualHost('test-virtual-host')

        then:
        configuration.getHost() == 'test-host'
        configuration.getUsername() == 'test-user'
        configuration.getPassword() == 'test-password'
        configuration.getAutomaticReconnect() == false
        configuration.getIsDefault() == true
        configuration.getName() == 'test-connection-name'
        configuration.getPort() == 10000
        configuration.getRequestedHeartbeat() == 1000
        configuration.getSsl() == true
        configuration.getThreads() == 10
        configuration.getVirtualHost() == 'test-virtual-host'
    }
}
