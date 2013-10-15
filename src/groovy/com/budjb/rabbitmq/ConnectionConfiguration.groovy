package com.budjb.rabbitmq

import groovy.util.ConfigObject;

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

class ConnectionConfiguration {
    /**
     * RabbitMQ host
     */
    public String host

    /**
     * RabbitMQ port
     */
    public int port = 5672

    /**
     * RabbitMQ username
     */
    public String username

    /**
     * RabbitMQ password
     */
    public String password

    /**
     * RabbitMQ virtual host
     */
    public String virtualHost = ''

    /**
     * Constructor that parses the configuration for RabbitMQ's connection properties.
     *
     * @param configuration
     */
    public ConnectionConfiguration(ConfigObject configuration) {
        // Make sure we have a configuration
        if (!configuration) {
            throw new Exception('RabbitMQ configuration is missing')
        }

        // Load the configuration
        host = configuration.host ?: null
        if (configuration.port) {
            port = configuration.port.toInteger()
        }
        username = configuration.username ?: null
        password = configuration.password ?: null
        virtualHost = configuration.virtualHost ?: ''

        // Ensure we have all we need to continue
        if (!host || !username || !password) {
            throw new Exception('The host, username, and password configuration options are required for RabbitMQ')
        }
    }

    /**
     * Returns a connection instance based on this context's configuration properties.
     *
     * @return
     */
    public Connection getConnection() {
        // Create the connection factory
        ConnectionFactory factory = new ConnectionFactory()

        // Configure it
        factory.username = username
        factory.password = password
        factory.port = port
        factory.host = host
        factory.virtualHost = virtualHost

        return factory.newConnection()
    }
}
