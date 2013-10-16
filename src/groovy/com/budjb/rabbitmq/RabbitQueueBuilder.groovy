package com.budjb.rabbitmq

import com.rabbitmq.client.Channel
import grails.util.Holders
import org.apache.log4j.Logger

/**
 * This class is based off of the queue builder present in the official Grails RabbitMQ plugin.
 */
class RabbitQueueBuilder {
    /**
     * Logger
     */
    private static Logger log = Logger.getLogger(RabbitQueueBuilder)

    /**
     * Current exchange marker
     */
    private Map currentExchange

    /**
     * RabbitMQ context bean
     */
    private RabbitContext rabbitContext

    /**
     * Constructor
     *
     * @param context
     */
    public RabbitQueueBuilder(RabbitContext context) {
        rabbitContext = context
    }

    /**
     * Handles queue definitions
     *
     * @param method
     * @param args
     */
    void queue(Map parameters) {
        // Grab required parameters
        String name = parameters['name']
        boolean autoDelete = Boolean.valueOf(parameters['autoDelete'])
        boolean exclusive = Boolean.valueOf(parameters['exclusive'])
        boolean durable = Boolean.valueOf(parameters['durable'])
        Map arguments = (parameters['arguments'] instanceof Map) ? parameters['arguments'] : [:]

        // Ensure we have a name
        if (!parameters['name']) {
            throw new RuntimeException("name is required to declare a queue")
        }

        // Grab a channel
        Channel channel = getChannel()

        // Declare the queue
        try {
            channel.queueDeclare(name, durable, exclusive, autoDelete, arguments)
        }
        finally {
            if (channel.isOpen()) {
                channel.close()
            }
        }

        // If we are nested inside of an exchange definition, create
        // a binding between the queue and the exchange.
        if (currentExchange) {
            bindQueue(parameters, currentExchange)
        }
    }

    /**
     * Binds a queue to an exchange.
     *
     * @param queue
     * @param exchange
     */
    void bindQueue(Map queue, Map exchange) {
        // Track our binding variables
        String routingKey = ''
        Map arguments = [:]

        // Set the binding based on the exchange type
        switch (currentExchange['type']) {
            case 'direct':
                if (queue['binding'] && !(queue['binding'] instanceof String)) {
                    throw new RuntimeException("binding for queue '${queue['name']}' to direct exchange '${currentExchange['name']}' must be a string")
                }

                routingKey = queue['binding'] ?: queue['name']
                break

            case 'fanout':
                routingKey = ''
                break

            case 'headers':
                if (!(queue['binding'] instanceof Map)) {
                    throw new RuntimeException("binding for queue '${queue['name']}' to headers exchange '${currentExchange['name']}' must be declared and must be a map")
                }
                if (!queue['match'] || !(queue['match'] in ['any', 'all'])) {
                    throw new RuntimeException("binding for queue '${queue['name']}' to headers exchange '${exchange['name']}' must have a match type declared ('any' or 'all')")
                }

                arguments = queue['binding'] + ['x-match': queue['match']]
                break

            case 'topic':
                if (!(queue['binding'] instanceof String)) {
                    throw new RuntimeException("binding for queue '${queue['name']}' to topic exchange '${currentExchange['name']}' must be declared and must be a string")
                }

                routingKey = queue['binding']
                break
        }

        // Grab a channel
        Channel channel = getChannel()

        // Bind the queue
        try {
            channel.queueBind(queue['name'], currentExchange['name'], routingKey, arguments)
        }
        finally {
            if (channel.isOpen()) {
                channel.close()
            }
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

        // Grab a channel
        Channel channel = getChannel()

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
            currentExchange = parameters
            closure = closure.clone()
            closure.delegate = this
            closure()
            currentExchange = null
        }
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

    /**
     * Retrieves a new channel from the rabbit context.
     *
     * @return
     */
    private Channel getChannel() {
        return rabbitContext.createChannel()
    }
}
