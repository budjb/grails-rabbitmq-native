
package com.budjb.rabbitmq.test
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.context.ApplicationContext

import com.budjb.rabbitmq.RabbitContext
import com.budjb.rabbitmq.RabbitContextImpl
import com.budjb.rabbitmq.RabbitQueueBuilder
import com.budjb.rabbitmq.connection.ConnectionConfiguration
import com.budjb.rabbitmq.connection.ConnectionContext
import com.budjb.rabbitmq.connection.ConnectionManager
import com.budjb.rabbitmq.consumer.RabbitConsumerManager
import com.budjb.rabbitmq.converter.MessageConverterManager
import com.budjb.rabbitmq.exception.InvalidConfigurationException
import com.budjb.rabbitmq.exception.MissingConfigurationException

import spock.lang.Specification

class RabbitContextSpec extends Specification {
    GrailsApplication grailsApplication
    MessageConverterManager messageConverterManager
    ConnectionManager connectionManager
    RabbitConsumerManager rabbitConsumerManager
    RabbitContext rabbitContext
    RabbitQueueBuilder rabbitQueueBuilder

    def setup() {
        grailsApplication = Mock(GrailsApplication)
        messageConverterManager = Mock(MessageConverterManager)
        connectionManager = Mock(ConnectionManager)
        rabbitConsumerManager = Mock(RabbitConsumerManager)
        rabbitQueueBuilder = Mock(RabbitQueueBuilder)

        rabbitContext = new RabbitContextImpl()
        rabbitContext.setGrailsApplication(grailsApplication)
        rabbitContext.setMessageConverterManager(messageConverterManager)
        rabbitContext.setConnectionManager(connectionManager)
        rabbitContext.setRabbitConsumerManager(rabbitConsumerManager)
        rabbitContext.setRabbitQueueBuilder(rabbitQueueBuilder)
    }

    def 'Basic test of start/stop/restart functionality'() {
        when:
        rabbitContext.load()

        then:
        1 * connectionManager.load()
        1 * messageConverterManager.load()
        1 * rabbitConsumerManager.load()

        when:
        rabbitContext.start()

        then:
        1 * connectionManager.open()
        1 * connectionManager.start()

        when:
        rabbitContext.start(true)

        then:
        1 * connectionManager.open()
        0 * connectionManager.start()

        when:
        rabbitContext.startConsumers()

        then:
        1 * connectionManager.start()

        when:
        rabbitContext.stop()

        then:
        1 * connectionManager.reset()
        1 * messageConverterManager.reset()
    }

    def 'Ensure createChannel() is passed through to the connectionManager'() {
        when:
        rabbitContext.createChannel()

        then:
        1 * connectionManager.createChannel()

        when:
        rabbitContext.createChannel('test-connection')

        then:
        1 * connectionManager.createChannel('test-connection')
    }
}
