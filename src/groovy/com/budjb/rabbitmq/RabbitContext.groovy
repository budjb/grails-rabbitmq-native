/*
 * Copyright 2015 Bud Byrd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.budjb.rabbitmq

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.context.ApplicationContext

import com.budjb.rabbitmq.connection.ConnectionContext
import com.budjb.rabbitmq.connection.ConnectionManager
import com.budjb.rabbitmq.consumer.ConsumerManager
import com.budjb.rabbitmq.converter.*

import com.rabbitmq.client.Channel

public interface RabbitContext {
    /**
     * Loads the configuration and registers any consumers or converters.
     */
    public void load()

    /**
     * Starts the RabbitMQ system. This includes connecting to any configured
     * RabbitMQ brokers and setting up any consumer channels.
     */
    public void start()

    /**
     * Starts the RabbitMQ system. This includes connecting to any configured
     * RabbitMQ brokers and optionally setting up any consumer channels.
     *
     * @param skipConsumers Whether to skip connecting consumer channels.
     */
    public void start(boolean skipConsumers)

    /**
     * Disconnects all consumer channels and closes any open RabbitMQ broker connections.
     */
    public void stop()

    /**
     * Stops the RabbitMQ service, reloads configuration, and starts services again.
     */
    public void restart()

    /**
     * Registers a message converter.
     *
     * @param converter
     */
    public void registerMessageConverter(MessageConverter converter)

    /**
     * Registers a consumer.
     *
     * @param candidate
     */
    public void registerConsumer(Object candidate)

    /**
     * Starts the consumers separately from the rest of the RabbitMQ service.
     * This is useful for delaying the start of the RabbitMQ services.
     */
    public void startConsumers()

    /**
     * Creates a channel with the default connection.
     *
     * @return
     */
    public Channel createChannel()

    /**
     * Creates a channel with the specified connection.
     *
     * @return
     */
    public Channel createChannel(String connectionName)

    /**
     * Returns the ConnectionContext associated with the default connection.
     *
     * @return
     */
    public ConnectionContext getConnection()

    /**
     * Returns the ConnectionContext with the specified connection name.
     *
     * @param name
     * @return
     */
    public ConnectionContext getConnection(String name)

    /**
     * Sets the message converter manager.
     *
     * @param messageConverterManager
     */
    public void setMessageConverterManager(MessageConverterManager messageConverterManager)

    /**
     * Sets the application context.
     */
    public void setApplicationContext(ApplicationContext applicationContext)

    /**
     * Sets the connection manager.
     */
    public void setConnectionManager(ConnectionManager connectionManager)

    /**
     * Sets the rabbit consumer manager.
     */
    public void setConsumerManager(ConsumerManager consumerManager)

    /**
     * Sets the rabbit queue builder.
     */
    public void setQueueBuilder(QueueBuilder queueBuilder)
}
