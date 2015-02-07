
package com.budjb.rabbitmq.test.consumer
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClass
import org.springframework.context.ApplicationContext

import com.budjb.rabbitmq.RabbitContext
import com.budjb.rabbitmq.RabbitMessagePublisher
import com.budjb.rabbitmq.connection.ConnectionContext
import com.budjb.rabbitmq.connection.ConnectionManager
import com.budjb.rabbitmq.consumer.ConsumerAdapter
import com.budjb.rabbitmq.consumer.RabbitConsumerManager
import com.budjb.rabbitmq.converter.MessageConverterManager

import spock.lang.Specification

class RabbitConsumerManagerSpec extends Specification {
    GrailsApplication grailsApplication
    Object persistenceInterceptor
    MessageConverterManager messageConverterManager
    RabbitMessagePublisher rabbitMessagePublisher
    ConnectionManager connectionManager
    RabbitConsumerManager rabbitConsumerManager
    ApplicationContext applicationContext

    def setup() {
        grailsApplication = Mock(GrailsApplication)
        persistenceInterceptor = null
        connectionManager = Mock(ConnectionManager)
        messageConverterManager = Mock(MessageConverterManager)
        rabbitMessagePublisher = Mock(RabbitMessagePublisher)
        applicationContext = Mock(ApplicationContext)

        rabbitConsumerManager = new RabbitConsumerManager()
        rabbitConsumerManager.grailsApplication = grailsApplication
        rabbitConsumerManager.persistenceInterceptor = persistenceInterceptor
        rabbitConsumerManager.messageConverterManager = messageConverterManager
        rabbitConsumerManager.rabbitMessagePublisher = rabbitMessagePublisher
        rabbitConsumerManager.connectionManager = connectionManager
        rabbitConsumerManager.applicationContext = applicationContext
    }

    def 'Ensure setGrailsApplication(GrailsApplication) sets the property correctly'() {
        setup:
        GrailsApplication grailsApplication = Mock(GrailsApplication)

        when:
        rabbitConsumerManager.setGrailsApplication(grailsApplication)

        then:
        rabbitConsumerManager.grailsApplication == grailsApplication
    }

    def 'Ensure setApplicationContext(ApplicationContext) sets the property correctly'() {
        setup:
        ApplicationContext applicationContext = Mock(ApplicationContext)

        when:
        rabbitConsumerManager.setApplicationContext(applicationContext)

        then:
        rabbitConsumerManager.applicationContext == applicationContext
    }

    def 'Ensure setMessageConverterManager(MessageConverterManager) sets the property correctly'() {
        setup:
        MessageConverterManager messageConverterManager = Mock(MessageConverterManager)

        when:
        rabbitConsumerManager.setMessageConverterManager(messageConverterManager)

        then:
        rabbitConsumerManager.messageConverterManager == messageConverterManager
    }

    def 'Ensure setConnectionManager(ConnectionManager) sets the property correctly'() {
        setup:
        ConnectionManager connectionManager = Mock(ConnectionManager)

        when:
        rabbitConsumerManager.setConnectionManager(connectionManager)

        then:
        rabbitConsumerManager.connectionManager == connectionManager
    }

    def 'Ensure setPersistenceInterceptor() sets the property correctly'() {
        setup:
        def persistenceInterceptor = new Expando()

        when:
        rabbitConsumerManager.setPersistenceInterceptor(persistenceInterceptor)

        then:
        rabbitConsumerManager.persistenceInterceptor == persistenceInterceptor
    }

    def 'Ensure setRabbitMessagePublisher(RabbitMessagePublisher) sets the property correctly.'() {
        setup:
        RabbitMessagePublisher rabbitMessagePublisher = Mock(RabbitMessagePublisher)

        when:
        rabbitConsumerManager.setRabbitMessagePublisher(rabbitMessagePublisher)

        then:
        rabbitConsumerManager.rabbitMessagePublisher == rabbitMessagePublisher
    }

    def 'Ensure proper objects are injected into new adapters'() {
        setup:
        Expando consumer = new Expando()

        when:
        ConsumerAdapter adapter = rabbitConsumerManager.createConsumerAdapter(consumer)

        then:
        adapter.grailsApplication == grailsApplication
        adapter.consumer == consumer
        adapter.persistenceInterceptor == persistenceInterceptor
        adapter.messageConverterManager == messageConverterManager
        adapter.rabbitMessagePublisher == rabbitMessagePublisher
        adapter.connectionManager == connectionManager
    }

    def 'Test load/registering of consumer artefacts'() {
        setup:
        GrailsClass artefact1 = Mock(GrailsClass)
        artefact1.getFullName() >> 'consumer1'
        GrailsClass artefact2 = Mock(GrailsClass)
        artefact2.getFullName() >> 'consumer2'
        Consumer1 consumer1 = new Consumer1()
        Consumer2 consumer2 = new Consumer2()
        applicationContext.getBean('consumer1') >> consumer1
        applicationContext.getBean('consumer2') >> consumer2
        grailsApplication.getArtefacts('MessageConsumer') >> [artefact1, artefact2]
        ConnectionContext connectionContext = Mock(ConnectionContext)
        connectionManager.getConnection(*_) >> connectionContext

        when:
        rabbitConsumerManager.load()

        then:
        2 * connectionContext.registerConsumer(_)
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
