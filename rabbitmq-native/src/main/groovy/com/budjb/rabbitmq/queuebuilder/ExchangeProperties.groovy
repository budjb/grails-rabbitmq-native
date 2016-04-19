package com.budjb.rabbitmq.queuebuilder

import com.budjb.rabbitmq.exception.InvalidConfigurationException

class ExchangeProperties extends ConfigurationProperties {
    /**
     * Exchange arguments (see RabbitMQ documentation).
     */
    Map arguments = [:]

    /**
     * Whether the queue should auto delete itself once no queues are bound to it.
     */
    boolean autoDelete = false

    /**
     * Whether the exchange is durable.
     */
    boolean durable = false

    /**
     * Name of the exchange.
     */
    String name

    /**
     * Type of the exchange.
     */
    ExchangeType type

    /**
     * Name of the connection to create the exchange with. No value uses the default connection.
     */
    String connection

    /**
     * Constructor.
     *
     * @param name
     * @param configuration
     */
    ExchangeProperties(String name, Map configuration) {
        this.name = name

        arguments = parseConfigOption(Map, configuration.arguments, arguments)
        autoDelete = parseConfigOption(Boolean, configuration.autoDelete, autoDelete)
        durable = parseConfigOption(Boolean, configuration.durable, durable)
        type = ExchangeType.lookup(parseConfigOption(String, configuration['type']))
        connection = parseConfigOption(String, configuration.connection, connection)
    }

    /**
     * Determines if the minimum requirements of this configuration set have been met and can be considered valid.
     *
     * @return
     */
    @Override
    void validate() {
        if (!name) {
            throw new InvalidConfigurationException("exchange name is required")
        }
        if (!type) {
            throw new InvalidConfigurationException("exchange type is required")
        }
    }
}
