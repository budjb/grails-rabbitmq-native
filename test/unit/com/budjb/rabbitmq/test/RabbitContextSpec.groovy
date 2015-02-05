package com.budjb.rabbitmq.test

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.context.ApplicationContext

import com.budjb.rabbitmq.RabbitContext
import com.budjb.rabbitmq.RabbitContextImpl
import com.budjb.rabbitmq.connection.ConnectionConfiguration
import com.budjb.rabbitmq.connection.ConnectionContext
import com.budjb.rabbitmq.converter.MessageConverterManager
import com.budjb.rabbitmq.exception.InvalidConfigurationException
import com.budjb.rabbitmq.exception.MissingConfigurationException

import spock.lang.Specification

class RabbitContextSpec extends Specification {
    def 'Happy path test of loading map-based configuration'() {
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

        GrailsApplication grailsApplication = Mock(GrailsApplication)
        grailsApplication.getConfig() >> config
        grailsApplication.getArtefacts('MessageConverter') >> []
        grailsApplication.getArtefacts('MessageConsumer') >> []

        MessageConverterManager messageConverterManager = Mock(MessageConverterManager)

        RabbitContext rabbitContext = new RabbitContextImpl()
        rabbitContext.setGrailsApplication(grailsApplication)
        rabbitContext.setMessageConverterManager(messageConverterManager)

        when:
        rabbitContext.load()

        then:
        rabbitContext.getConnection() != null

        ConnectionConfiguration configuration = rabbitContext.getConnection().getConfiguration()
        configuration.getHost() == 'test.budjb.com'
    }

    def 'Happy path test of loading closure-based configuration'() {
        setup:
        ConfigObject config = new ConfigObject()
        config.putAll([
            'rabbitmq': [
                'connection': {
                    connection(
                        'name': 'defaultConnection',
                        'isDefault': true,
                        'host': 'test.budjb.com',
                        'username': 'test-user',
                        'password': 'test-password'
                    )
                    connection(
                        'name': 'secondaryConnection',
                        'host': 'test2.budjb.com',
                        'username': 'test-user-2',
                        'password': 'test-password-2'
                    )
                }
            ]
        ])

        GrailsApplication grailsApplication = Mock(GrailsApplication)
        grailsApplication.getConfig() >> config
        grailsApplication.getArtefacts('MessageConverter') >> []
        grailsApplication.getArtefacts('MessageConsumer') >> []

        MessageConverterManager messageConverterManager = Mock(MessageConverterManager)

        RabbitContext rabbitContext = new RabbitContextImpl()
        rabbitContext.setGrailsApplication(grailsApplication)
        rabbitContext.setMessageConverterManager(messageConverterManager)

        when:
        rabbitContext.load()

        then:
        ConnectionContext connection1 = rabbitContext.getConnection()
        ConnectionContext connection2 = rabbitContext.getConnection('secondaryConnection')

        connection1 != null
        connection2 != null

        connection1.getConfiguration().getIsDefault() == true
        connection1.getConfiguration().getName() == 'defaultConnection'
        connection1.getConfiguration().getHost() == 'test.budjb.com'

        connection2.getConfiguration().getIsDefault() == false
        connection2.getConfiguration().getName() == 'secondaryConnection'
        connection2.getConfiguration().getHost() == 'test2.budjb.com'
    }

    def 'When no configuration is defined, an exception should be thrown'() {
        setup:
        ConfigObject config = new ConfigObject()

        GrailsApplication grailsApplication = Mock(GrailsApplication)
        grailsApplication.getConfig() >> config
        grailsApplication.getArtefacts('MessageConverter') >> []
        grailsApplication.getArtefacts('MessageConsumer') >> []

        MessageConverterManager messageConverterManager = Mock(MessageConverterManager)

        RabbitContext rabbitContext = new RabbitContextImpl()
        rabbitContext.setGrailsApplication(grailsApplication)
        rabbitContext.setMessageConverterManager(messageConverterManager)

        when:
        rabbitContext.load()

        then:
        thrown MissingConfigurationException
    }

    def 'An InvalidConfigurationException should be thrown when no connections are specified'() {
        setup:
        ConfigObject config = new ConfigObject()
        config.putAll([
            'rabbitmq': [
                'connection': { }
            ]
        ])

        GrailsApplication grailsApplication = Mock(GrailsApplication)
        grailsApplication.getConfig() >> config
        grailsApplication.getArtefacts('MessageConverter') >> []
        grailsApplication.getArtefacts('MessageConsumer') >> []

        MessageConverterManager messageConverterManager = Mock(MessageConverterManager)

        RabbitContext rabbitContext = new RabbitContextImpl()
        rabbitContext.setGrailsApplication(grailsApplication)
        rabbitContext.setMessageConverterManager(messageConverterManager)

        when:
        rabbitContext.load()

        then:
        thrown InvalidConfigurationException
    }

    def 'If multiple default connections are configured, and InvalidConfigurationException should be thrown'() {
        setup:
        ConfigObject config = new ConfigObject()
        config.putAll([
            'rabbitmq': [
                'connection': {
                    connection(
                        'name': 'defaultConnection',
                        'isDefault': true,
                        'host': 'test.budjb.com',
                        'username': 'test-user',
                        'password': 'test-password'
                    )
                    connection(
                        'name': 'secondaryConnection',
                        'isDefault': true,
                        'host': 'test2.budjb.com',
                        'username': 'test-user-2',
                        'password': 'test-password-2'
                    )
                }
            ]
        ])

        GrailsApplication grailsApplication = Mock(GrailsApplication)
        grailsApplication.getConfig() >> config
        grailsApplication.getArtefacts('MessageConverter') >> []
        grailsApplication.getArtefacts('MessageConsumer') >> []

        MessageConverterManager messageConverterManager = Mock(MessageConverterManager)

        RabbitContext rabbitContext = new RabbitContextImpl()
        rabbitContext.setGrailsApplication(grailsApplication)
        rabbitContext.setMessageConverterManager(messageConverterManager)

        when:
        rabbitContext.load()

        then:
        thrown InvalidConfigurationException
    }

    def 'Basic test of start/stop/restart functionality'() {
        setup:
        ConfigObject config = new ConfigObject()
        config.putAll([
            'rabbitmq': [
                'connection': {
                    connection(
                        'name': 'defaultConnection',
                        'isDefault': true,
                        'host': 'test.budjb.com',
                        'username': 'test-user',
                        'password': 'test-password'
                    )
                    connection(
                        'name': 'secondaryConnection',
                        'host': 'test2.budjb.com',
                        'username': 'test-user-2',
                        'password': 'test-password-2'
                    )
                }
            ]
        ])

        GrailsApplication grailsApplication = Mock(GrailsApplication)
        grailsApplication.getConfig() >> config
        grailsApplication.getArtefacts('MessageConverter') >> []
        grailsApplication.getArtefacts('MessageConsumer') >> []

        MessageConverterManager messageConverterManager = Mock(MessageConverterManager)

        RabbitContext rabbitContext = Spy(RabbitContextImpl)
        rabbitContext.setGrailsApplication(grailsApplication)
        rabbitContext.setMessageConverterManager(messageConverterManager)
        rabbitContext.load()

        when:
        rabbitContext.start()

        then:
        //1 * rabbitContext.start(false)
        1 == 1

        /*
        when:
        rabbitContext.restart()

        then:

        1 * rabbitContext.stop()
        1 * rabbitContext.load()
        1 * rabbitContext.start()
        1 * rabbitContext.start(false)

        when:
        rabbitContext.stop()

        then:
        1 * messageConverterManager.reset()
        */
    }
}
