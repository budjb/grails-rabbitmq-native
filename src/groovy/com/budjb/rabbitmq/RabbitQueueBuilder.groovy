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
    private String currentExchange

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
        String exchange = parameters['exchange']
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
        else if (exchange) {
            bindQueue(parameters, exchange)
        }
    }

    /**
     * Binds a queue to an exchange.
     *
     * @param queue
     * @param exchange
     */
    void bindQueue(Map queue, String exchange) {
        // Grab a channel
        Channel channel = getChannel()

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
            currentExchange = parameters['name']
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
