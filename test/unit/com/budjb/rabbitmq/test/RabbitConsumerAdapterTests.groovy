package com.budjb.rabbitmq.test

import static org.mockito.Mockito.*

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.junit.Before
import org.mockito.Mockito

import com.budjb.rabbitmq.ConnectionContext
import com.budjb.rabbitmq.ConsumerConfiguration
import com.budjb.rabbitmq.MessageContext
import com.budjb.rabbitmq.MessageConverterManager
import com.budjb.rabbitmq.RabbitConsumerAdapter
import com.budjb.rabbitmq.RabbitContext
import com.budjb.rabbitmq.converter.GStringMessageConverter
import com.budjb.rabbitmq.converter.IntegerMessageConverter
import com.budjb.rabbitmq.converter.ListMessageConverter
import com.budjb.rabbitmq.converter.MapMessageConverter
import com.budjb.rabbitmq.converter.StringMessageConverter
import com.rabbitmq.client.BasicProperties
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope


class RabbitConsumerAdapterTests {
    MessageConverterManager messageConverterManager

    @Before
    void setup() {
        messageConverterManager = new MessageConverterManager()
        messageConverterManager.registerMessageConverter(new IntegerMessageConverter())
        messageConverterManager.registerMessageConverter(new MapMessageConverter())
        messageConverterManager.registerMessageConverter(new ListMessageConverter())
        messageConverterManager.registerMessageConverter(new GStringMessageConverter())
        messageConverterManager.registerMessageConverter(new StringMessageConverter())
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
     * Test that the adapter accurately handles configurations defined locally inside a consumer.
     */
    void testLocalConfiguration() {
        // Mock the grails applicaton config
        GrailsApplication grailsApplication = mock(GrailsApplication)
        when(grailsApplication.getConfig()).thenReturn(new ConfigObject())

        //  Mock a rabbit context
        RabbitContext rabbitContext = mock(RabbitContext)

        // Create the adapter
        RabbitConsumerAdapter adapter = new RabbitConsumerAdapter.RabbitConsumerAdapterBuilder().build {
            delegate.consumer = new LocalConfigConsumer()
            delegate.grailsApplication = grailsApplication
            delegate.rabbitContext = rabbitContext
            delegate.messageConverterManager= messageConverterManager
        }

        // Get the configuration
        ConsumerConfiguration configuration = adapter.configuration

        // Validate the consumer name
        assert adapter.getConsumerName() == 'LocalConfigConsumer'

        // Validate the configuration options
        assert configuration.queue == 'local-config-queue'
        assert configuration.consumers == 5
        assert configuration.retry == false

        // Validate that the consumer is valid
        assert adapter.isConsumerValid() == true
    }

    /**
     * Test that the adapter accurately handles configurations defined centrally in the application configuration.
     */
    void testCentralConfiguration() {
        // Mock the grails applicaton config
        GrailsApplication grailsApplication = mock(GrailsApplication)
        when(grailsApplication.getConfig()).thenReturn(new ConfigObject([
            'rabbitmq': [
                'consumers': [
                    'CentralConfigConsumer': [
                        'queue': 'central-config-queue',
                        'consumers': 10,
                        'retry': true
                    ]
                ]
            ]
        ]))

        //  Mock a rabbit context
        RabbitContext rabbitContext = mock(RabbitContext)

        // Create the adapter
        RabbitConsumerAdapter adapter = new RabbitConsumerAdapter.RabbitConsumerAdapterBuilder().build {
            delegate.consumer = new CentralConfigConsumer()
            delegate.grailsApplication = grailsApplication
            delegate.rabbitContext = rabbitContext
            delegate.messageConverterManager= messageConverterManager
        }

        // Get the configuration
        ConsumerConfiguration configuration = adapter.configuration

        // Validate the consumer name
        assert adapter.getConsumerName() == 'CentralConfigConsumer'

        // Validate the configuration options
        assert configuration.queue == 'central-config-queue'
        assert configuration.consumers == 10
        assert configuration.retry == true

        // Validate that the consumer is valid
        assert adapter.isConsumerValid() == true
    }

    /**
     * Test most of the callbacks.
     */
    void testCallbacks() {
        // Mock the grails applicaton config
        GrailsApplication grailsApplication = mock(GrailsApplication)
        when(grailsApplication.getConfig()).thenReturn(new ConfigObject([
            'rabbitmq': [
                'consumers': [
                    'CallbackConsumer': [
                        'queue': 'callback-queue'
                    ]
                ]
            ]
        ]))

        //  Mock a rabbit context
        RabbitContext rabbitContext = mock(RabbitContext)

        // Create a mocked consumer
        CallbackConsumer consumer = mock(CallbackConsumer)

        // Create the adapter
        RabbitConsumerAdapter adapter = spy(new RabbitConsumerAdapter.RabbitConsumerAdapterBuilder().build {
            delegate.consumer = consumer
            delegate.grailsApplication = grailsApplication
            delegate.rabbitContext = rabbitContext
            delegate.messageConverterManager= messageConverterManager
        })

        // Mock the consumer name (sigh)
        doReturn('CallbackConsumer').when(adapter).getConsumerName()

        // Mock a message context
        MessageContext context = new MessageContext(
            channel: mock(Channel),
            consumerTag: '',
            envelope: mock(Envelope),
            properties: mock(BasicProperties),
            body: 'test body'.getBytes(),
            connectionContext: mock(ConnectionContext)
        )

        // Hand off the message to the adapter
        adapter.deliverMessage(context)

        // Ensure that the callbacks were called
        verify(consumer, times(1)).onReceive(context)
        verify(consumer, times(1)).onSuccess(context)
        verify(consumer, times(1)).onComplete(context)
        verify(consumer, never()).onFailure(context)
    }

    /**
     * Test most of the callbacks.
     */
    void testFailureCallbacks() {
        // Mock the grails applicaton config
        GrailsApplication grailsApplication = mock(GrailsApplication)
        when(grailsApplication.getConfig()).thenReturn(new ConfigObject([
            'rabbitmq': [
                'consumers': [
                    'CallbackConsumer': [
                        'queue': 'callback-queue'
                    ]
                ]
            ]
        ]))

        //  Mock a rabbit context
        RabbitContext rabbitContext = mock(RabbitContext)

        // Create a mocked consumer
        CallbackConsumer consumer = mock(CallbackConsumer)

        // Force an exception when the handler is called
        when(consumer.handleMessage(any(), any())).thenThrow(new RuntimeException())

        // Create the adapter
        RabbitConsumerAdapter adapter = spy(new RabbitConsumerAdapter.RabbitConsumerAdapterBuilder().build {
            delegate.consumer = consumer
            delegate.grailsApplication = grailsApplication
            delegate.rabbitContext = rabbitContext
            delegate.messageConverterManager= messageConverterManager
        })

        // Mock the consumer name (sigh)
        doReturn('CallbackConsumer').when(adapter).getConsumerName()

        // Mock a message context
        MessageContext context = new MessageContext(
            channel: mock(Channel),
            consumerTag: '',
            envelope: mock(Envelope),
            properties: mock(BasicProperties),
            body: 'test body'.getBytes(),
            connectionContext: mock(ConnectionContext)
        )

        // Hand off the message to the adapter
        adapter.deliverMessage(context)

        // Ensure that the callbacks were called
        verify(consumer, times(1)).onReceive(context)
        verify(consumer, never()).onSuccess(context)
        verify(consumer, times(1)).onComplete(context)
        verify(consumer, times(1)).onFailure(context)
    }

    void testStart() {
        // Mock the grails applicaton config
        GrailsApplication grailsApplication = mock(GrailsApplication)
        when(grailsApplication.getConfig()).thenReturn(new ConfigObject())

        //  Mock a rabbit context
        RabbitContext rabbitContext = mock(RabbitContext)

        // Mock a connection context
        ConnectionContext context = mock(ConnectionContext)
        context.name = 'default'

        // Create a consumer
        LocalConfigConsumer consumer = new LocalConfigConsumer()

        // Create the adapter
        RabbitConsumerAdapter adapter = new RabbitConsumerAdapter.RabbitConsumerAdapterBuilder().build {
            delegate.consumer = consumer
            delegate.grailsApplication = grailsApplication
            delegate.rabbitContext = rabbitContext
            delegate.messageConverterManager= messageConverterManager
        }

        // Start the adapter
        adapter.start()

    }
}
