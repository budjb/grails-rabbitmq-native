package com.budjb.rabbitmq.test

import org.codehaus.groovy.grails.commons.GrailsApplication

import spock.lang.Specification

import com.budjb.rabbitmq.*
import com.budjb.rabbitmq.connection.ConnectionConfiguration
import com.budjb.rabbitmq.connection.ConnectionContext
import com.budjb.rabbitmq.connection.ConnectionManager

import com.budjb.rabbitmq.consumer.ConsumerConfiguration
import com.budjb.rabbitmq.consumer.RabbitConsumerAdapter
import com.budjb.rabbitmq.consumer.RabbitConsumerManager
import com.budjb.rabbitmq.converter.*
import com.rabbitmq.client.BasicProperties
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import com.rabbitmq.client.impl.AMQImpl.Queue.DeclareOk


class RabbitConsumerAdapterSpec extends Specification {
    /**
     * Message converter manager.
     */
    MessageConverterManager messageConverterManager

    /**
     * Sets up the mocked environment for each test.
     */
    def setup() {
        messageConverterManager = new MessageConverterManager()
        messageConverterManager.registerMessageConverter(new IntegerMessageConverter())
        messageConverterManager.registerMessageConverter(new MapMessageConverter())
        messageConverterManager.registerMessageConverter(new ListMessageConverter())
        messageConverterManager.registerMessageConverter(new GStringMessageConverter())
        messageConverterManager.registerMessageConverter(new StringMessageConverter())
    }

    /**
     * Test that the adapter accurately handles configurations defined locally inside a consumer.
     */
    void 'Validate the parsed consumer configuration for a locally defined configuration'() {
        setup:
        // Mock the grails application bean and a blank config
        GrailsApplication grailsApplication = Mock(GrailsApplication)
        grailsApplication.getConfig() >> new ConfigObject()

        //  Mock the connection  manager
        ConnectionManager connectionManager = Mock(ConnectionManager)

        // Mock the consumer adapter factory
        RabbitConsumerManager rabbitConsumerManager = new RabbitConsumerManager()
        rabbitConsumerManager.grailsApplication = grailsApplication
        rabbitConsumerManager.connectionManager = connectionManager
        rabbitConsumerManager.messageConverterManager = messageConverterManager

        when:
        // Create the adapter
        RabbitConsumerAdapter adapter = rabbitConsumerManager.createConsumerAdapter(new LocalConfigConsumer())

        // Get the configuration
        ConsumerConfiguration configuration = adapter.configuration

        then:
        // Validate the consumer
        adapter.getConsumerName() == 'LocalConfigConsumer'

        // Validate the configuration options
        configuration.queue == 'local-config-queue'
        configuration.consumers == 5
        configuration.retry == false

        // Validate that the consumer is valid
        adapter.isConsumerValid() == true
    }

    /**
     * Test that the adapter accurately handles configurations defined centrally in the application configuration.
     */
    void 'Validate the parsed consumer configuration for a centrally defined configuration'() {
        setup:
        // Mock the grails applicaton config
        GrailsApplication grailsApplication = Mock(GrailsApplication)
        grailsApplication.getConfig() >> new ConfigObject([
            'rabbitmq': [
                'consumers': [
                    'CentralConfigConsumer': [
                        'queue': 'central-config-queue',
                        'consumers': 10,
                        'retry': true
                    ]
                ]
            ]
        ])

        //  Mock the connection  manager
        ConnectionManager connectionManager = Mock(ConnectionManager)

        // Mock the consumer adapter factory
        RabbitConsumerManager rabbitConsumerManager = new RabbitConsumerManager()
        rabbitConsumerManager.grailsApplication = grailsApplication
        rabbitConsumerManager.connectionManager = connectionManager
        rabbitConsumerManager.messageConverterManager = messageConverterManager

        when:
        // Create the adapter
        RabbitConsumerAdapter adapter = rabbitConsumerManager.createConsumerAdapter(new CentralConfigConsumer())

        // Get the configuration
        ConsumerConfiguration configuration = adapter.configuration

        then:
        // Validate the consumer name
        adapter.getConsumerName() == 'CentralConfigConsumer'

        // Validate the configuration options
        configuration.queue == 'central-config-queue'
        configuration.consumers == 10
        configuration.retry == true

        // Validate that the consumer is valid
        adapter.isConsumerValid() == true
    }

    /**
     * Test most of the callbacks.
     */
    void 'Verify that the proper consumer callbacks are invoked for a successful message'() {
        setup:
        // Mock the grails applicaton config
        GrailsApplication grailsApplication = Mock(GrailsApplication)
        grailsApplication.getConfig() >> new ConfigObject([
            'rabbitmq': [
                'consumers': [
                    'CallbackConsumer': [
                        'queue': 'callback-queue'
                    ]
                ]
            ]
        ])

        //  Mock the connection  manager
        ConnectionManager connectionManager = Mock(ConnectionManager)

        // Create a mocked consumer
        CallbackConsumer consumer = Mock(CallbackConsumer)

        // Mock a persistence intercepter
        def persistenceInterceptor = Mock(PersistenceInterceptor)

        // Create the adapter
        RabbitConsumerAdapter adapter = Spy(RabbitConsumerAdapter, constructorArgs: [
            consumer, grailsApplication, connectionManager, messageConverterManager, persistenceInterceptor, null
        ])

        // Mock the consumer name (sigh)
        adapter.getConsumerName() >> 'CallbackConsumer'

        // Mock a message context
        MessageContext context = new MessageContext(
            channel: Mock(Channel),
            consumerTag: '',
            envelope: Mock(Envelope),
            properties: Mock(BasicProperties),
            body: 'test body'.getBytes(),
            connectionContext: Mock(ConnectionContext)
        )

        when:
        // Hand off the message to the adapter
        adapter.deliverMessage(context)

        then:
        // Ensure that the callbacks were called
        1 * consumer.onReceive(context)
        1 * consumer.onSuccess(context)
        1 * consumer.onComplete(context)
        0 * consumer.onFailure(context)

        1 * persistenceInterceptor.init()
        1 * persistenceInterceptor.flush()
        1 * persistenceInterceptor.destroy()
    }

    /**
     * Test most of the callbacks.
     */
    void 'Verify that the proper consumer callbacks are invoked for an unsuccessful message'() {
        setup:
        // Mock the grails applicaton config
        GrailsApplication grailsApplication = Mock(GrailsApplication)
        grailsApplication.getConfig() >> new ConfigObject([
            'rabbitmq': [
                'consumers': [
                    'CallbackConsumer': [
                        'queue': 'callback-queue'
                    ]
                ]
            ]
        ])

        //  Mock the connection  manager
        ConnectionManager connectionManager = Mock(ConnectionManager)

        // Create a mocked consumer
        CallbackConsumer consumer = Mock(CallbackConsumer)

        // Force an exception when the handler is called
        consumer.handleMessage(*_) >> { throw new RuntimeException() }

        // Create the adapter
        RabbitConsumerAdapter adapter = Spy(RabbitConsumerAdapter, constructorArgs: [
            consumer, grailsApplication, connectionManager, messageConverterManager, null, null
        ])

        // Mock the consumer name (sigh)
        adapter.getConsumerName() >> 'CallbackConsumer'

        // Mock a message context
        MessageContext context = new MessageContext(
            channel: Mock(Channel),
            consumerTag: '',
            envelope: Mock(Envelope),
            properties: Mock(BasicProperties),
            body: 'test body'.getBytes(),
            connectionContext: Mock(ConnectionContext)
        )

        when:
        // Hand off the message to the adapter
        adapter.deliverMessage(context)

        then:
        // Ensure that the callbacks were called
        1 * consumer.onReceive(context)
        0 * consumer.onSuccess(context)
        1 * consumer.onComplete(context)
        1 * consumer.onFailure(context)
    }

    void 'Start a basic consumer'() {
        setup:
        // Mock the grails applicaton config
        GrailsApplication grailsApplication = Mock(GrailsApplication)
        grailsApplication.getConfig() >> new ConfigObject()

        // Mock a connection configuration
        ConnectionConfiguration connectionConfiguration = Mock(ConnectionConfiguration)
        connectionConfiguration.getName() >> 'default'

        // Mock a connection context
        ConnectionContext context = Mock(ConnectionContext)
        context.getConfiguration() >> connectionConfiguration
        context.createChannel(*_) >> {
            Channel channel = Mock(Channel)
            return channel
        }

        //  Mock a connection manager that returns the mocked connection context
        ConnectionManager connectionManager = Mock(ConnectionManager)
        connectionManager.getConnection(*_) >> context

        // Create a consumer
        LocalConfigConsumer consumer = new LocalConfigConsumer()

        // Mock the consumer adapter factory
        RabbitConsumerManager rabbitConsumerManager = new RabbitConsumerManager()
        rabbitConsumerManager.grailsApplication = grailsApplication
        rabbitConsumerManager.connectionManager = connectionManager
        rabbitConsumerManager.messageConverterManager = messageConverterManager

        when:
        // Create the adapter
        RabbitConsumerAdapter adapter = rabbitConsumerManager.createConsumerAdapter(consumer)

        // Start the adapter
        adapter.start()

        then:
        adapter.consumers.size() == 5
    }

    def 'If the consumer has already been started and tried to start again, throw an IllegalStateException'() {
        setup:
        // Mock the grails applicaton config
        GrailsApplication grailsApplication = Mock(GrailsApplication)
        grailsApplication.getConfig() >> new ConfigObject()

        // Mock a connection configuration
        ConnectionConfiguration connectionConfiguration = Mock(ConnectionConfiguration)
        connectionConfiguration.getName() >> 'default'

        // Mock a connection context
        ConnectionContext context = Mock(ConnectionContext)
        context.getConfiguration() >> connectionConfiguration
        context.createChannel(*_) >> {
            Channel channel = Mock(Channel)
            return channel
        }

        //  Mock a connection manager that returns the mocked connection context
        ConnectionManager connectionManager = Mock(ConnectionManager)
        connectionManager.getConnection(*_) >> context

        // Create a consumer
        LocalConfigConsumer consumer = new LocalConfigConsumer()

        // Mock the consumer adapter factory
        RabbitConsumerManager rabbitConsumerManager = new RabbitConsumerManager()
        rabbitConsumerManager.grailsApplication = grailsApplication
        rabbitConsumerManager.connectionManager = connectionManager
        rabbitConsumerManager.messageConverterManager = messageConverterManager

        when:
        // Create the adapter
        RabbitConsumerAdapter adapter = rabbitConsumerManager.createConsumerAdapter(consumer)

        // Start the adapter twice
        adapter.start()
        adapter.start()

        then:
        thrown IllegalStateException
    }

    def 'If using an exchange and binding, there should only be one consumer created'() {
        setup:
        // Mock the grails applicaton config
        GrailsApplication grailsApplication = Mock(GrailsApplication)
        grailsApplication.getConfig() >> new ConfigObject()

        // Create a mocked channel
        Channel channel = Mock(Channel)
        channel.queueDeclare(*_) >> { new DeclareOk('temp-queue', 0, 0) }

        // Mock a connection configuration
        ConnectionConfiguration connectionConfiguration = Mock(ConnectionConfiguration)
        connectionConfiguration.getName() >> 'default'

        // Mock a connection context
        ConnectionContext context = Mock(ConnectionContext)
        context.getConfiguration() >> connectionConfiguration
        context.createChannel(*_) >> channel

        //  Mock a connection manager that returns the mocked connection context
        ConnectionManager connectionManager = Mock(ConnectionManager)
        connectionManager.getConnection(*_) >> context

        // Create a consumer
        SubscriberConsumer consumer = new SubscriberConsumer()

        // Mock the consumer adapter factory
        RabbitConsumerManager rabbitConsumerManager = new RabbitConsumerManager()
        rabbitConsumerManager.grailsApplication = grailsApplication
        rabbitConsumerManager.connectionManager = connectionManager
        rabbitConsumerManager.messageConverterManager = messageConverterManager

        when:
        // Create the adapter
        RabbitConsumerAdapter adapter = rabbitConsumerManager.createConsumerAdapter(consumer)

        // Start the adapter
        adapter.start()

        then:
        adapter.consumers.size() == 1
        1 * channel.basicConsume('temp-queue', _, _)
    }

    /**
     * Used to test a consumer with a local configuration.
     */
    class LocalConfigConsumer {
        static rabbitConfig = [
            'queue': 'local-config-queue',
            'consumers': 5,
            'retry': false
        ]

        def handleMessage(def body, def context) {

        }
    }

    /**
     * Used to test a consumer with a central configuration.
     */
    class CentralConfigConsumer {
        def handleMessage(def body, def context) {

        }
    }

    /**
     * Used to test callbacks.
     */
    class CallbackConsumer {
        def handleMessage(def body, def context) {

        }

        void onReceive(def context) {

        }

        def onSuccess(def context) {

        }

        def onComplete(def context) {

        }

        def onFailure(def context) {

        }
    }

    /**
     * Used to test subscriber-based configs.
     */
    class SubscriberConsumer {
        static rabbitConfig = [
            'exchange': 'test-exchange',
            'binding': 'test-binding'
        ]

        def handleMessage(def body, def messageContext) {

        }
    }

    class PersistenceInterceptor {
        void init() { }
        void flush() { }
        void destroy() { }
    }
}
