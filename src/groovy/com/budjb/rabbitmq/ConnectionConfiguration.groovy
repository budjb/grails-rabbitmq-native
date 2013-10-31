package com.budjb.rabbitmq

import groovy.util.ConfigObject;
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.apache.log4j.Logger

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

class ConnectionConfiguration {
    /**
     * Logger.
     */
    Logger log = Logger.getLogger(getClass())

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
     * The maximum number of concurrent consumer threads that are processed.
     *
     * 5 is the RabbitMQ default.
     */
    public int threads = 5

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
        if (configuration.port instanceof Integer) {
            port = configuration.port
        }
        username = configuration.username ?: null
        password = configuration.password ?: null
        virtualHost = configuration.virtualHost ?: ''
        threads = configuration.threads ?: threads

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

        // Create the thread pool service
        ExecutorService executorService = Executors.newFixedThreadPool(threads)

        return factory.newConnection(executorService)
    }
}
