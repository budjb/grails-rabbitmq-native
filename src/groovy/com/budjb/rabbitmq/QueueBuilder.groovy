/*
 * Copyright 2014-2015 Bud Byrd
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
package com.budjb.rabbitmq

import com.budjb.rabbitmq.connection.ConnectionContext
import com.budjb.rabbitmq.connection.ConnectionManager
import com.rabbitmq.client.Channel
import org.apache.log4j.Logger
import org.codehaus.groovy.grails.commons.GrailsApplication

/**
 * This class is based off of the queue builder present in the official Grails RabbitMQ plugin.
 */
class QueueBuilder {
    /**
     * Connection manager.
     */
    ConnectionManager connectionManager

    /**
     * Grails application bean.
     */
    GrailsApplication grailsApplication

    /**
     * Configure any defined exchanges and queues.
     */
    void configureQueues() {
        // Skip if the config isn't defined
        if (!(grailsApplication.config.rabbitmq?.queues instanceof Closure)) {
            return
        }

        // Grab the config closure
        Closure config = grailsApplication.config.rabbitmq.queues

        // Create the queue builder
        QueueBuilderDelegate queueBuilderDelegate = new QueueBuilderDelegate()

        // Run the config
        config = config.clone()
        config.delegate = queueBuilderDelegate
        config.resolveStrategy = Closure.DELEGATE_FIRST
        config()
    }

    /**
     * Class that does the work of building exchanges and queues.
     */
    private class QueueBuilderDelegate {
        /**
         * Logger
         */
        private static Logger log = Logger.getLogger(QueueBuilder)

        /**
         * Current exchange marker
         */
        private String currentExchange = null

        /**
         * Current connection to create exchanges/queues against
         */
        private ConnectionContext currentConnection = null

        /**
         * RabbitMQ context bean
         */
        private RabbitContext rabbitContext = null

        /**
         * Handles queue definitions
         *
         * @param method
         * @param args
         */
        void queue(Map parameters) {
            // Grab required parameters
            String name = parameters['name']
            String exchange = parameters['exchange']
            boolean autoDelete = Boolean.valueOf(parameters['autoDelete'])
            boolean exclusive = Boolean.valueOf(parameters['exclusive'])
            boolean durable = Boolean.valueOf(parameters['durable'])
            Map arguments = (parameters['arguments'] instanceof Map) ? parameters['arguments'] : [:]

            // Ensure we have a name
            if (!parameters['name']) {
                throw new RuntimeException("name is required to declare a queue")
            }

            // Determine the connection
            ConnectionContext connection = currentConnection
            if (!currentConnection) {
                String connectionName = parameters['connection'] ?: null
                connection = getConnection(connectionName)
                if (!connection) {
                    if (!connectionName) {
                        throw new RuntimeException("no default connection found")
                    }
                    else {
                        throw new RuntimeException("no connection with name '${connectionName}' found")
                    }
                }
            }

            // Grab a channel
            Channel channel = connection.createChannel()

            // Declare the queue
            try {
                channel.queueDeclare(name, durable, exclusive, autoDelete, arguments)

                // If we are nested inside of an exchange definition, create
                // a binding between the queue and the exchange.
                if (currentExchange) {
                    bindQueue(parameters, currentExchange, channel)
                }
                else if (exchange) {
                    bindQueue(parameters, exchange, channel)
                }
            }
            finally {
                if (channel.isOpen()) {
                    channel.close()
                }
            }
        }

        /**
         * Binds a queue to an exchange.
         *
         * @param queue
         * @param exchange
         */
        void bindQueue(Map queue, String exchange, Channel channel) {
            if (queue['binding'] instanceof String) {
                channel.queueBind(queue['name'], exchange, queue['binding'])
            }
            else if (queue['binding'] instanceof Map) {
                if (!(queue['match'] in ['any', 'all'])) {
                    log.warn("skipping queue binding of queue \"${queue['name']}\" to headers exchange because the \"match\" property was not set or not one of (\"any\", \"all\")")
                    return
                }
                channel.queueBind(queue['name'], exchange, '', queue['binding'] + ['x-match': queue['match']])
            }
            else {
                channel.queueBind(queue['name'], exchange, '')
            }
        }

        /**
         * Defines a new exchange.
         *
         * @param args The properties of the exchange.
         * @param closure An optional closure that includes queue definitions that will be bound to this exchange.
         */
        void exchange(Map parameters, Closure closure = null) {
            // Make sure we're not already in an exchange call
            if (currentExchange) {
                throw new RuntimeException("cannot declare an exchange within another exchange")
            }

            // Get parameters
            String name = parameters['name']
            String type = parameters['type']
            boolean autoDelete = Boolean.valueOf(parameters['autoDelete'])
            boolean durable = Boolean.valueOf(parameters['durable'])

            // Grab the extra arguments
            Map arguments = (parameters['arguments'] instanceof Map) ? parameters['arguments'] : [:]

            // Ensure we have a name
            if (!name) {
                throw new RuntimeException("an exchange name must be provided")
            }

            // Ensure we have a type
            if (!type) {
                throw new RuntimeException("a type must be provided for the exchange '${name}'")
            }

            // Determine the connection
            ConnectionContext connection = currentConnection
            if (!currentConnection) {
                String connectionName = parameters['connection'] ?: null
                connection = getConnection(connectionName)
                if (!connection) {
                    if (!connectionName) {
                        throw new RuntimeException("no default connection found")
                    }
                    else {
                        throw new RuntimeException("no connection with name '${connectionName}' found")
                    }
                }
            }

            // Grab a channel
            Channel channel = connection.createChannel()

            // Declare the exchange
            try {
                channel.exchangeDeclare(name, type, durable, autoDelete, arguments)
            }
            finally {
                if (channel.isOpen()) {
                    channel.close()
                }
            }

            // Run the closure if given
            if (closure) {
                boolean resetConnection = (currentConnection == null)

                currentExchange = parameters['name']
                currentConnection = connection
                closure = closure.clone()
                closure.delegate = this
                closure()
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
            // Sanity check
            if (currentConnection != null) {
                throw new RuntimeException("unexpected connection in the queue configuration; there is a current connection already open")
            }

            // Find the connection
            ConnectionContext context = getConnection(name)
            if (!context) {
                throw new RuntimeException("no connection with name '${name}' found")
            }

            // Store the context
            currentConnection = context

            // Run the closure
            closure = closure.clone()
            closure.delegate = this
            closure()

            // Clear the context
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

        private ConnectionContext getConnection(String name) {
            return QueueBuilder.this.connectionManager.getContext(name)
        }
    }
}
