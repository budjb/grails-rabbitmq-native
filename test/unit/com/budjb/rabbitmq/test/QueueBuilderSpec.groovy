package com.budjb.rabbitmq.test

import org.codehaus.groovy.grails.commons.GrailsApplication

import com.budjb.rabbitmq.QueueBuilder
import com.budjb.rabbitmq.connection.ConnectionContext
import com.budjb.rabbitmq.connection.ConnectionManager
import com.rabbitmq.client.Channel

import spock.lang.Specification

class QueueBuilderSpec extends Specification {
    QueueBuilder queueBuilder
    GrailsApplication grailsApplication
    ConnectionManager connectionManager

    def setup() {
        grailsApplication = Mock(GrailsApplication)

        connectionManager = Mock(ConnectionManager)

        queueBuilder = new QueueBuilder()
        queueBuilder.grailsApplication = grailsApplication
        queueBuilder.connectionManager = connectionManager
    }

    def 'Ensure setConnectionManager(ConnectiomManager) sets the property correctly'() {
        setup:
        ConnectionManager connectionManager = Mock(ConnectionManager)

        when:
        queueBuilder.setConnectionManager(connectionManager)

        then:
        queueBuilder.connectionManager == connectionManager
    }

    def 'Ensure setGrailsApplication(GrailsApplication) sets the property correctly'() {
        setup:
        GrailsApplication grailsApplication = Mock(GrailsApplication)

        when:
        queueBuilder.setGrailsApplication(grailsApplication)

        then:
        queueBuilder.grailsApplication == grailsApplication
    }

    def 'Test queue creation on a single connection'() {
        setup:
        ConfigObject configuration = new ConfigObject()
        configuration.putAll([
            'rabbitmq': [
                'queues': {
                    queue(name: 'test-queue-1', durable: true)
                    exchange(name: 'test-exchange-1', type: 'topic', durable: true) {
                        queue(name: 'test-queue-2', durable: true, binding: 'test.binding.#')
                    }
                }
            ]
        ])
        grailsApplication.getConfig() >> configuration
        Channel channel = Mock(Channel)
        channel.isOpen() >> true
        ConnectionContext connectionContext = Mock(ConnectionContext)
        connectionContext.createChannel() >> channel
        connectionManager.getConnection(_) >> connectionContext

        when:
        queueBuilder.configureQueues()

        then:
        3 * connectionContext.createChannel() >> channel
        1 * channel.queueDeclare('test-queue-1', true, false, false, [:])
        1 * channel.exchangeDeclare('test-exchange-1', 'topic', true, false, [:])
        1 * channel.queueDeclare('test-queue-2', true, false, false, [:])
        1 * channel.queueBind('test-queue-2', 'test-exchange-1', 'test.binding.#')
        3 * channel.close()
    }

    def 'Test configuration containing a connection'() {
        setup:
        ConfigObject configuration = new ConfigObject()
        configuration.putAll([
            'rabbitmq': [
                'queues': {
                    connection('secondaryConnection') {
                        exchange(name: 'test-exchange', type: 'topic', durable: true) {
                            queue(name: 'test-queue', durable: true, binding: 'test.binding.#')
                        }
                    }
                }
            ]
        ])
        grailsApplication.getConfig() >> configuration
        Channel channel1 = Mock(Channel)
        channel1.isOpen() >> true
        Channel channel2 = Mock(Channel)
        channel2.isOpen() >> true
        ConnectionContext connectionContext1 = Mock(ConnectionContext)
        connectionContext1.createChannel() >> channel1
        ConnectionContext connectionContext2 = Mock(ConnectionContext)
        connectionContext2.createChannel() >> channel2
        connectionManager.getConnection(null) >> connectionContext1
        connectionManager.getConnection('secondaryConnection') >> connectionContext2

        when:
        queueBuilder.configureQueues()

        then:
        2 * connectionContext2.createChannel() >> channel2
        1 * channel2.exchangeDeclare('test-exchange', 'topic', true, false, [:])
        1 * channel2.queueDeclare('test-queue', true, false, false, [:])
        1 * channel2.queueBind('test-queue', 'test-exchange', 'test.binding.#')
        2 * channel2.close()

        0 * connectionContext1.createChannel() >> channel1
        0 * channel2.close()
    }

    def 'Validate all queue options are respected'() {
        setup:
        ConfigObject configuration = new ConfigObject()
        configuration.putAll([
            'rabbitmq': [
                'queues': {
                    queue(
                        'name': 'test-queue',
                        'exchange': 'test-exchange',
                        'autoDelete': true,
                        'exclusive': true,
                        'durable': true,
                        'connection': 'test-connection',
                        'binding': 'test-binding',
                        'arguments': ['foo': 'bar']
                    )
                }
            ]
        ])
        grailsApplication.getConfig() >> configuration
        Channel channel = Mock(Channel)
        channel.isOpen() >> true
        ConnectionContext connectionContext = Mock(ConnectionContext)
        connectionContext.createChannel() >> channel
        connectionManager.getConnection('test-connection') >> connectionContext

        when:
        queueBuilder.configureQueues()

        then:
        1 * connectionContext.createChannel() >> channel
        1 * channel.queueDeclare('test-queue', true, true, true, ['foo': 'bar'])
        1 * channel.queueBind('test-queue', 'test-exchange', 'test-binding')
        1 * channel.close()
    }

    def 'If a queue binding is a Map, test the header type binding'() {
        setup:
        ConfigObject configuration = new ConfigObject()
        configuration.putAll([
            'rabbitmq': [
                'queues': {
                    queue(
                        'name': 'test-queue',
                        'exchange': 'test-exchange',
                        'autoDelete': true,
                        'exclusive': true,
                        'durable': true,
                        'connection': 'test-connection',
                        'binding': ['foo': 'bar'],
                        'match': 'all',
                        'arguments': ['foo': 'bar']
                    )
                }
            ]
        ])
        grailsApplication.getConfig() >> configuration
        Channel channel = Mock(Channel)
        channel.isOpen() >> true
        ConnectionContext connectionContext = Mock(ConnectionContext)
        connectionContext.createChannel() >> channel
        connectionManager.getConnection('test-connection') >> connectionContext

        when:
        queueBuilder.configureQueues()

        then:
        1 * connectionContext.createChannel() >> channel
        1 * channel.queueDeclare('test-queue', true, true, true, ['foo': 'bar'])
        1 * channel.queueBind('test-queue', 'test-exchange', '', ['foo': 'bar', 'x-match': 'all'])
        1 * channel.close()
    }

    def 'Validate all exchange options are respected'() {
        setup:
        ConfigObject configuration = new ConfigObject()
        configuration.putAll([
            'rabbitmq': [
                'queues': {
                    exchange(
                        'name': 'test-exchange',
                        'type': 'direct',
                        'autoDelete': true,
                        'durable': true,
                        'connection': 'test-connection',
                        'arguments': ['foo': 'bar']
                    )
                }
            ]
        ])
        grailsApplication.getConfig() >> configuration
        Channel channel = Mock(Channel)
        channel.isOpen() >> true
        ConnectionContext connectionContext = Mock(ConnectionContext)
        connectionContext.createChannel() >> channel
        connectionManager.getConnection('test-connection') >> connectionContext

        when:
        queueBuilder.configureQueues()

        then:
        1 * connectionContext.createChannel() >> channel
        1 * channel.exchangeDeclare('test-exchange', 'direct', true, true, ['foo': 'bar'])
        1 * channel.close()
    }
}
