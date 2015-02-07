
package com.budjb.rabbitmq.test
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.context.ApplicationContext

import com.budjb.rabbitmq.RabbitContext
import com.budjb.rabbitmq.RabbitContextImpl
import com.budjb.rabbitmq.QueueBuilder
import com.budjb.rabbitmq.connection.ConnectionConfiguration
import com.budjb.rabbitmq.connection.ConnectionContext
import com.budjb.rabbitmq.connection.ConnectionManager
import com.budjb.rabbitmq.consumer.RabbitConsumerManager
import com.budjb.rabbitmq.converter.MessageConverterManager
import com.budjb.rabbitmq.exception.InvalidConfigurationException
import com.budjb.rabbitmq.exception.MissingConfigurationException

import spock.lang.Specification

class RabbitContextImplSpec extends Specification {
    GrailsApplication grailsApplication
    MessageConverterManager messageConverterManager
    ConnectionManager connectionManager
    RabbitConsumerManager rabbitConsumerManager
    RabbitContext rabbitContext
    QueueBuilder queueBuilder

    def setup() {
        grailsApplication = Mock(GrailsApplication)
        messageConverterManager = Mock(MessageConverterManager)
        connectionManager = Mock(ConnectionManager)
        rabbitConsumerManager = Mock(RabbitConsumerManager)
        queueBuilder = Mock(QueueBuilder)

        rabbitContext = new RabbitContextImpl()
        rabbitContext.setMessageConverterManager(messageConverterManager)
        rabbitContext.setConnectionManager(connectionManager)
        rabbitContext.setRabbitConsumerManager(rabbitConsumerManager)
        rabbitContext.setQueueBuilder(queueBuilder)
    }

    def 'Ensure createChannel() is proxied through to the connectionManager'() {
        when:
        rabbitContext.createChannel()

        then:
        1 * connectionManager.createChannel()
    }

    def 'Ensure createChannel(String) is proxied through to the connection manager'() {
        when:
        rabbitContext.createChannel('test-connection')

        then:
        1 * connectionManager.createChannel('test-connection')
    }

    def 'Ensure getConnection() is proxied through to the connection manager'() {
        when:
        rabbitContext.getConnection()

        then:
        1 * connectionManager.getConnection()
    }

    def 'Ensure getConnection(String) is proxied through to the connection manager'() {
        when:
        rabbitContext.getConnection("test")

        then:
        1 * connectionManager.getConnection("test")
    }

    def 'When load() is called, all of the inject managers should be loaded'() {
        when:
        rabbitContext.load()

        then:
        1 * connectionManager.load()
        1 * messageConverterManager.load()
        1 * rabbitConsumerManager.load()
    }

    def 'Ensure registerConsumer(Object) is proxied to the rabbit consumer manager'() {
        when:
        rabbitContext.registerConsumer(null)

        then:
        1 * rabbitConsumerManager.registerConsumer(null)
    }

    def 'Ensure registerMessageConverter(MessageConverter) is proxied to the rabbit consumer manager'() {
        when:
        rabbitContext.registerMessageConverter(null)

        then:
        1 * messageConverterManager.registerMessageConverter(null)
    }

    def 'When stop() is called, the connection and message converter managers should be reset'() {
        when:
        rabbitContext.stop()

        then:
        1 * connectionManager.reset()
        1 * messageConverterManager.reset()
    }

    def 'When start() is called, connections should be opened, queues should be configured, and consumers should be started'() {
        when:
        rabbitContext.start()

        then:
        1 * connectionManager.open()
        1 * queueBuilder.configureQueues()
        1 * connectionManager.start()
    }

    def 'When start(true) is called, connections should be opened, queues should be configured, and consumers should not be started'() {
        when:
        rabbitContext.start(true)

        then:
        1 * connectionManager.open()
        1 * queueBuilder.configureQueues()
        0 * connectionManager.start()
    }

    def 'Ensure the proper interactions for restart (stop/load/start)'() {
        when:
        rabbitContext.restart()

        then:
        connectionManager.reset()
        messageConverterManager.reset()

        then:
        connectionManager.load()
        messageConverterManager.load()
        rabbitConsumerManager.load()

        then:
        1 * connectionManager.open()
        1 * queueBuilder.configureQueues()
        1 * connectionManager.start()
    }

    def 'Ensure startConsumers() is proxied to the connection manager'() {
        when:
        rabbitContext.startConsumers()

        then:
        1 * connectionManager.start()
    }

    def 'Ensure setApplicationContext(ApplicationContext) sets the property correctly'() {
        setup:
        ApplicationContext applicationContext = Mock(ApplicationContext)

        when:
        rabbitContext.setApplicationContext(applicationContext)

        then:
        rabbitContext.applicationContext == applicationContext
    }

    def 'Ensure setConnectionManager(ConnectionManager) sets the property correctly'() {
        setup:
        ConnectionManager connectionManager = Mock(ConnectionManager)

        when:
        rabbitContext.setConnectionManager(connectionManager)

        then:
        rabbitContext.connectionManager == connectionManager
    }

    def 'Ensure setMessageConverterManager(MessageConverterManager) sets the property correctly'() {
        setup:
        MessageConverterManager messageConverterManager = Mock(MessageConverterManager)

        when:
        rabbitContext.setMessageConverterManager(messageConverterManager)

        then:
        rabbitContext.messageConverterManager == messageConverterManager
    }

    def 'Ensure setRabbitConsumerManager(RabbitConsumerManager) sets the proeprty correctly'() {
        setup:
        RabbitConsumerManager rabbitConsumerManager = Mock(RabbitConsumerManager)

        when:
        rabbitContext.setRabbitConsumerManager(rabbitConsumerManager)

        then:
        rabbitContext.rabbitConsumerManager == rabbitConsumerManager
    }

    def 'Ensure setQueueBuilder(QueueBuilder) sets the property correctly'() {
        setup:
        QueueBuilder queueBuilder = Mock(QueueBuilder)

        when:
        rabbitContext.setQueueBuilder(queueBuilder)

        then:
        rabbitContext.queueBuilder == queueBuilder
    }
}
