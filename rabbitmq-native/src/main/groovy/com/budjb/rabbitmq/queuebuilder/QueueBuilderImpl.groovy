/*
 * Copyright 2014-2016 Bud Byrd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.budjb.rabbitmq.queuebuilder

import com.budjb.rabbitmq.connection.ConnectionContext
import com.budjb.rabbitmq.connection.ConnectionManager
import com.budjb.rabbitmq.exception.InvalidConfigurationException
import com.rabbitmq.client.Channel
import grails.core.GrailsApplication
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

/**
 * This class is based off of the queue builder present in the official Grails RabbitMQ plugin.
 */
@Slf4j
class QueueBuilderImpl implements QueueBuilder {
    /**
     * Connection manager.
     */
    @Autowired
    ConnectionManager connectionManager

    /**
     * Grails application bean.
     */
    @Autowired
    GrailsApplication grailsApplication

    /**
     * Queue configurations parsed from the application configuration.
     */
    List<QueueProperties> queues = []

    /**
     * Exchange configurations parsed from the application configuration.
     */
    List<ExchangeProperties> exchanges = []

    /**
     * Configure any defined exchanges and queues.
     */
    void configure() {
        queues.clear()
        exchanges.clear()

        def configuration = grailsApplication.config.rabbitmq.queues

        if (configuration instanceof Closure) {
            log.warn("closure-based configuration for queues and exchanges is deprecated")
            call(configuration, new ClosureDelegate())
        }
        else if (configuration instanceof Map) {
            parse(grailsApplication.config.rabbitmq as Map)
        }
        else {
            throw new InvalidConfigurationException("queue/exchanges configuration is invalid")
        }


        queues*.validate()
        exchanges*.validate()

        configureExchanges()
        configureQueues()
        configureBindings()
    }

    /**
     * Run the given closure with the given delegate.
     *
     * @param closure
     * @param delegate
     */
    void call(Closure closure, Object delegate) {
        closure = closure.clone() as Closure
        closure.delegate = delegate
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.call()
    }

    /**
     * Parses the configuration.
     *
     * @param configuration
     */
    void parse(Map configuration) {
        if (configuration.containsKey('queues') && configuration.queues instanceof Map) {
            parseQueues(configuration.queues as Map)
        }

        if (configuration.containsKey('exchanges') && configuration.exchanges instanceof Map) {
            parseExchanges(configuration.exchanges as Map)
        }
    }

    /**
     * Parses the queue configuration.
     *
     * @param queues
     */
    void parseQueues(Map queues) {
        queues.each { k, v ->
            if (!(k instanceof String)) {
                return
            }

            if (v == null) {
                v = [:]
            }
            else if (!(v instanceof Map)) {
                return
            }

            this.queues << new QueueProperties(k, v as Map)
        }
    }

    /**
     * Parses the exchange configuration.
     *
     * @param exchanges
     */
    void parseExchanges(Map exchanges) {
        exchanges.each { k, v ->
            if (!(k instanceof String)) {
                return
            }
            if (!(v instanceof Map)) {
                return
            }
            this.exchanges << new ExchangeProperties(k, v)
        }
    }

    /**
     * Creates/configures exchanges based on the exchange configurations.
     */
    void configureExchanges() {
        exchanges.each {
            configureExchange(it)
        }
    }

    /**
     * Creates/configures an exchange based on the given exchange configuration.
     *
     * @param properties
     */
    void configureExchange(ExchangeProperties properties) {
        Channel channel = getConnection(properties.getConnection()).createChannel()

        try {
            channel.exchangeDeclare(
                properties.name,
                properties.type.toString().toLowerCase(),
                properties.durable,
                properties.autoDelete,
                properties.arguments
            )
        }
        finally {
            if (channel.isOpen()) {
                channel.close()
            }
        }
    }

    /**
     * Creates/configures queues based on the queue configurations.
     */
    void configureQueues() {
        queues.each {
            configureQueue(it)
        }
    }

    /**
     * Creates/configures a queue based on the given queue configuration.
     *
     * @param properties
     */
    void configureQueue(QueueProperties properties) {
        Channel channel = getConnection(properties.getConnection()).createChannel()

        try {
            channel.queueDeclare(
                properties.name,
                properties.durable,
                properties.exclusive,
                properties.autoDelete,
                properties.arguments
            )
        }
        finally {
            if (channel.isOpen()) {
                channel.close()
            }
        }
    }

    /**
     * Binds queues to exchanges based on queue configurations.
     */
    void configureBindings() {
        queues.each {
            configureBindings(it)
        }
    }

    /**
     * Binds a queue to an exchange based on the given queue configuration.
     *
     * @param properties
     */
    void configureBindings(QueueProperties properties) {
        if (!properties.exchange) {
            return
        }

        Channel channel = getConnection(properties.getConnection()).createChannel()

        try {
            if (!properties.binding) {
                channel.queueBind(properties.name, properties.exchange, '')
            }
            else if (properties.binding instanceof String) {
                channel.queueBind(properties.name, properties.exchange, properties.binding as String)
            }
            else {
                Map binding = properties.binding
                binding.put('x-match', properties.match.toString().toLowerCase())
                channel.queueBind(properties.name, properties.exchange, '', binding)
            }
        }
        finally {
            if (channel.isOpen()) {
                channel.close()
            }
        }
    }

    /**
     * Returns the connection with the given name. If name is <code>null</code>, the default connection is returned.
     *
     * @param name
     * @return
     */
    ConnectionContext getConnection(String name) {
        if (!name) {
            return connectionManager.getContext()
        }
        else {
            return connectionManager.getContext(name)
        }
    }

    /**
     * Class that handles closure based configurations.
     */
    private class ClosureDelegate {
        /**
         * Current exchange marker.
         */
        private String currentExchange = null

        /**
         * Current connection marker.
         */
        private String currentConnection = null

        /**
         * Configures a queue.
         *
         * @param name
         * @param config
         */
        void queue(String name, Map config) {
            config.name = name
            queue(config)
        }

        /**
         * Handles queue definitions
         *
         * @param method
         * @param args
         */
        void queue(Map config) {
            if (currentConnection) {
                config.connection = currentConnection
            }
            if (currentExchange) {
                config.exchange = currentExchange
            }
            QueueBuilderImpl.this.queues << new QueueProperties(config.name?.toString(), config)
        }

        /**
         * Configures an exchange.
         *
         * @param name
         * @param parameters
         * @param closure
         */
        void exchange(String name, Map parameters, Closure closure = null) {
            parameters.name = parameters.name ?: name
            exchange(parameters, closure)
        }

        /**
         * Configures an exchange.
         *
         * @param args The properties of the exchange.
         * @param closure An optional closure that includes queue definitions that will be bound to this exchange.
         */
        void exchange(Map parameters, Closure closure = null) {
            if (currentExchange) {
                throw new RuntimeException("cannot declare an exchange within another exchange")
            }

            if (currentConnection) {
                parameters.connection = currentConnection
            }

            String name = parameters.name

            QueueBuilderImpl.this.exchanges << new ExchangeProperties(name, parameters)

            if (closure) {
                boolean resetConnection = (currentConnection == null)

                currentConnection = parameters.connection
                currentExchange = name

                QueueBuilderImpl.this.call(closure, this)

                currentExchange = null
                if (resetConnection) {
                    currentConnection = null
                }
            }
        }

        /**
         * Lets the exchange and queue methods know what connection to build against.
         *
         * @param name
         * @param closure
         */
        void connection(String name, Closure closure) {
            if (currentConnection != null) {
                throw new RuntimeException("unexpected connection in the queue configuration; the connection ${currentConnection} is already open")
            }

            currentConnection = name

            QueueBuilderImpl.this.call(closure, this)

            currentConnection = null
        }

        /**
         * Returns the name of the direct exchange type.
         *
         * @return
         */
        String getDirect() {
            return 'direct'
        }

        /**
         * Returns the name of the fanout exchange type.
         *
         * @return
         */
        String getFanout() {
            return 'fanout'
        }

        /**
         * Returns the name of the headers exchange type.
         *
         * @return
         */
        String getHeaders() {
            return 'headers'
        }

        /**
         * Returns the name of the topic exchange type.
         *
         * @return
         */
        String getTopic() {
            return 'topic'
        }

        /**
         * Returns the string representation of 'any', used in the match type for header exchanges.
         *
         * @return
         */
        String getAny() {
            return 'any'
        }

        /**
         * Returns the string representation of 'all', used in the match type for header exchanges.
         *
         * @return
         */
        String getAll() {
            return 'all'
        }
    }
}
