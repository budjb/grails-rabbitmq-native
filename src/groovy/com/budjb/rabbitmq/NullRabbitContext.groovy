package com.budjb.rabbitmq

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.context.ApplicationContext

import com.budjb.rabbitmq.connection.ConnectionContext
import com.budjb.rabbitmq.connection.ConnectionManager
import com.budjb.rabbitmq.consumer.RabbitConsumerManager

import com.budjb.rabbitmq.converter.*

import com.rabbitmq.client.Channel

/**
 * A null object implementation of the RabbitContext.
 * This is created when the plugin is disabled.
 */
class NullRabbitContext implements RabbitContext {
    /* (non-Javadoc)
     * @see com.budjb.rabbitmq.RabbitContext#load()
     */
    @Override
    public void load() {

    }

    /* (non-Javadoc)
     * @see com.budjb.rabbitmq.RabbitContext#start()
     */
    @Override
    public void start() {

    }

    /* (non-Javadoc)
     * @see com.budjb.rabbitmq.RabbitContext#start(boolean)
     */
    @Override
    public void start(boolean skipConsumers) {

    }

    /* (non-Javadoc)
     * @see com.budjb.rabbitmq.RabbitContext#stop()
     */
    @Override
    public void stop() {

    }

    /* (non-Javadoc)
     * @see com.budjb.rabbitmq.RabbitContext#restart()
     */
    @Override
    public void restart() {

    }

    /* (non-Javadoc)
     * @see com.budjb.rabbitmq.RabbitContext#registerMessageConverter(com.budjb.rabbitmq.MessageConverter)
     */
    @Override
    public void registerMessageConverter(MessageConverter converter) {

    }

    /* (non-Javadoc)
     * @see com.budjb.rabbitmq.RabbitContext#registerConsumer(com.budjb.rabbitmq.DefaultGrailsMessageConsumerClass)
     */
    @Override
    public void registerConsumer(Object candidate) {

    }

    /* (non-Javadoc)
     * @see com.budjb.rabbitmq.RabbitContext#startConsumers()
     */
    @Override
    public void startConsumers() {

    }

    /* (non-Javadoc)
     * @see com.budjb.rabbitmq.RabbitContext#createChannel()
     */
    @Override
    public Channel createChannel() {
        throw new UnsupportedOperationException('unable to create a new channel with a disabled RabbitContext')
    }

    /* (non-Javadoc)
     * @see com.budjb.rabbitmq.RabbitContext#createChannel(java.lang.String)
     */
    @Override
    public Channel createChannel(String connectionName) {
        throw new UnsupportedOperationException('unable to create a new channel with a disabled RabbitContext')
    }

    /* (non-Javadoc)
     * @see com.budjb.rabbitmq.RabbitContext#getConnection()
     */
    @Override
    public ConnectionContext getConnection() {
        throw new UnsupportedOperationException("unable to retrieve a connection with a disabled RabbitContext")
    }

    /* (non-Javadoc)
     * @see com.budjb.rabbitmq.RabbitContext#getConnection(java.lang.String)
     */
    @Override
    public ConnectionContext getConnection(String name) {
        throw new UnsupportedOperationException("unable to retrieve a connection with a disabled RabbitContext")
    }

    @Override
    public void setMessageConverterManager(MessageConverterManager messageConverterManager) {

    }

    @Override
    public void setGrailsApplication(GrailsApplication grailsApplication) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setConnectionManager(ConnectionManager connectionManager) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setRabbitConsumerManager(RabbitConsumerManager rabbitConsumerManager) {
    }
}
