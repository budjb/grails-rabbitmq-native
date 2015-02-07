package com.budjb.rabbitmq.consumer

import org.apache.log4j.Logger
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClass
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

import com.budjb.rabbitmq.RabbitContext
import com.budjb.rabbitmq.RabbitMessagePublisher
import com.budjb.rabbitmq.connection.ConnectionContext
import com.budjb.rabbitmq.connection.ConnectionManager
import com.budjb.rabbitmq.converter.MessageConverterManager

class RabbitConsumerManager implements ApplicationContextAware {
    /**
     * Grails application bean.
     */
    protected GrailsApplication grailsApplication

    /**
     * Hibernate object used to bind a session to the current thread.
     *
     * This will be null if Hibernate is not present.
     */
    protected Object persistenceInterceptor

    /**
     * Message converter manager.
     */
    protected MessageConverterManager messageConverterManager

    /**
     * Rabbit message publisher.
     */
    protected RabbitMessagePublisher rabbitMessagePublisher

    /**
     * Connection manager.
     */
    protected ConnectionManager connectionManager

    /**
     * Application context.
     */
    protected ApplicationContext applicationContext

    /**
     * Logger.
     */
    protected Logger log = Logger.getLogger(RabbitConsumerManager)

    /**
     * Creates a new RabbitConsumerAdapter.
     */
    public RabbitConsumerAdapter createConsumerAdapter(Object consumer) {
        return new RabbitConsumerAdapter(
            consumer,
            grailsApplication,
            connectionManager,
            messageConverterManager,
            persistenceInterceptor,
            rabbitMessagePublisher
        )
    }

    /**
     * Loads any message consumer artefacts.
     */
    public void load() {
        grailsApplication.getArtefacts('MessageConsumer').each { registerConsumer(it) }
    }

    /**
     * Registers a new message consumer.
     *
     * @param consumer
     * @return
     */
    public void registerConsumer(Object consumer) {
        // If the consumer is a grails artefact class, get its bean
        if (consumer instanceof GrailsClass) {
            consumer = applicationContext.getBean(consumer.fullName)
        }

        // Create the adapter
        RabbitConsumerAdapter adapter = createConsumerAdapter(consumer)

        // Find the appropriate connection context
        ConnectionContext context = connectionManager.getConnection(adapter.getConfiguration().getConnection())

        // Log a warning if a connection wasn't found
        if (!context) {
            log.warn('unable to register ${adapter.getConsumerName()} as a consumer because its connection could not be found')
            return
        }

        // Register the adapter
        context.registerConsumer(adapter)
    }

    /**
     * Sets the application context.
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext
    }

    /**
     * Sets the grails application bean.
     */
    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
    }

    /**
     * Sets the connection manager.
     */
    public void setConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager
    }

    /**
     * Sets the message converter manager.
     */
    public void setMessageConverterManager(MessageConverterManager messageConverterManager) {
        this.messageConverterManager = messageConverterManager
    }

    /**
     * Sets the persistence intercepter, if available.
     */
    public void setPersistenceInterceptor(def persistenceInterceptor) {
        this.persistenceInterceptor = persistenceInterceptor
    }

    /**
     * Sets the rabbit message builder.
     */
    public void setRabbitMessagePublisher(RabbitMessagePublisher rabbitMessagePublisher) {
        this.rabbitMessagePublisher = rabbitMessagePublisher
    }
}
