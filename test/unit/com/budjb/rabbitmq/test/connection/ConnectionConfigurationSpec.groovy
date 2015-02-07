package com.budjb.rabbitmq.test.connection

import com.budjb.rabbitmq.connection.ConnectionConfiguration
import com.rabbitmq.client.ConnectionFactory

import spock.lang.Specification

class ConnectionConfigurationSpec extends Specification {
    def 'Test default configuration options'() {
        setup:
        Map properties = [
            'host': 'test-host',
            'username': 'test-user',
            'password': 'test-password'
        ]

        when:
        ConnectionConfiguration connectionConfiguration = new ConnectionConfiguration(properties)

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
        when:
        new ConnectionConfiguration([:])

        then:
        thrown AssertionError
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
        ConnectionConfiguration connectionConfiguration = new ConnectionConfiguration(properties)

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
        ConnectionConfiguration configuration = new ConnectionConfiguration()

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
