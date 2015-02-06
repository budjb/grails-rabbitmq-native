package com.budjb.rabbitmq.test

import org.codehaus.groovy.grails.commons.GrailsApplication

import com.budjb.rabbitmq.connection.ConnectionContext
import com.budjb.rabbitmq.connection.ConnectionManager
import com.budjb.rabbitmq.exception.InvalidConfigurationException
import com.budjb.rabbitmq.exception.MissingConfigurationException

import spock.lang.Specification

class ConnectionManagerSpec extends Specification {
    GrailsApplication grailsApplication
    ConnectionManager connectionManager

    def setup() {
        grailsApplication = Mock(GrailsApplication)
        connectionManager = new ConnectionManager()
        connectionManager.grailsApplication = grailsApplication
    }

    def 'If no connection configuration is missing, a MissingConfigurationException should be thrown'() {
        setup:
        grailsApplication.getConfig() >> new ConfigObject()

        when:
        connectionManager.load()

        then:
        thrown MissingConfigurationException
    }

    def 'If the connection stanza is not a map or closure, an InvalidConfigurationException should be thrown'() {
        setup:
        ConfigObject config = new ConfigObject()
        config.putAll([
            'rabbitmq': [
                'connection': 'foobar'
            ]
        ])
        grailsApplication.getConfig() >> config

        when:
        connectionManager.load()

        then:
        thrown InvalidConfigurationException
    }

    def 'If the connection is empty, an InvalidConfigurationException should be thrown'() {
        setup:
        ConfigObject config = new ConfigObject()
        config.putAll([
            'rabbitmq': [
                'connection': { }
            ]
        ])
        grailsApplication.getConfig() >> config

        when:
        connectionManager.load()

        then:
        thrown InvalidConfigurationException
    }

    def 'Map configuration test'() {
        setup:
        ConfigObject config = new ConfigObject()
        config.putAll([
            'rabbitmq': [
                'connection': [
                    'host': 'test.budjb.com',
                    'username': 'test-user',
                    'password': 'test-password'
                ]
            ]
        ])
        grailsApplication.getConfig() >> config

        when:
        connectionManager.load()

        then:
        connectionManager.getConnection().getConfiguration().getHost() == 'test.budjb.com'
    }

    def 'Closure configuration test'() {
        setup:
        ConfigObject config = new ConfigObject()
        config.putAll([
            'rabbitmq': [
                'connection': {
                    connection(
                        'name': 'primaryConnection',
                        'isDefault': true,
                        'host': 'test.budjb.com',
                        'username': 'test-user',
                        'password': 'test-password'
                    )
                    connection(
                        'name': 'secondaryConnection',
                        'host': 'foo.budjb.com',
                        'username': 'test-user',
                        'password': 'test-password'
                    )
                }
            ]
        ])
        grailsApplication.getConfig() >> config

        when:
        connectionManager.load()

        then:
        connectionManager.getConnection().getConfiguration().getHost() == 'test.budjb.com'
        connectionManager.getConnection('primaryConnection').getConfiguration().getHost() == 'test.budjb.com'
        connectionManager.getConnection('secondaryConnection').getConfiguration().getHost() == 'foo.budjb.com'
    }

    def 'If more than one default connection is configured, an InvalidConfigurationException should be thrown'() {
        setup:
        ConfigObject config = new ConfigObject()
        config.putAll([
            'rabbitmq': [
                'connection': {
                    connection(
                        'name': 'primaryConnection',
                        'isDefault': true,
                        'host': 'test.budjb.com',
                        'username': 'test-user',
                        'password': 'test-password'
                    )
                    connection(
                        'name': 'secondaryConnection',
                        'isDefault': true,
                        'host': 'foo.budjb.com',
                        'username': 'test-user',
                        'password': 'test-password'
                    )
                }
            ]
        ])
        grailsApplication.getConfig() >> config

        when:
        connectionManager.load()

        then:
        thrown InvalidConfigurationException
    }

    def 'Validate open/start/reset functionality'() {
        setup:
        ConnectionContext connection1 = Mock(ConnectionContext)
        ConnectionContext connection2 = Mock(ConnectionContext)

        connectionManager.connections = [connection1, connection2]

        when:
        connectionManager.open()

        then:
        1 * connection1.openConnection()
        1 * connection2.openConnection()

        when:
        connectionManager.start()

        then:
        1 * connection1.startConsumers()
        1 * connection2.startConsumers()

        when:
        connectionManager.reset()

        then:
        1 * connection1.stopConsumers()
        1 * connection1.closeConnection()
        1 * connection2.stopConsumers()
        1 * connection2.closeConnection()
        connectionManager.connections.size() == 0
    }
}
