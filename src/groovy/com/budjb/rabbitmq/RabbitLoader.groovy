package com.budjb.rabbitmq

import org.apache.log4j.Logger
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClass
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

class RabbitLoader {
    private class ConnectionConfiguration {
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

    /**
     * Connection configuration
     */
    private ConnectionConfiguration connectionConfiguration

    /**
     * Grails application bean
     */
    public static GrailsApplication grailsApplication

    /**
     * Logger
     */
    private Logger log = Logger.getLogger(this.getClass())

    /**
     * Singleton instance of the driver.
     */
    private static RabbitLoader _instance

    /**
     * Connection to the RabbitMQ server
     */
    private Connection connection

    /**
     * List of active channels
     */
    private List<Channel> channels = []

    /**
     * Returns the singleton instance of this class.
     *
     * @return
     */
    public static RabbitLoader getInstance() {
        if (!_instance) {
            _instance = new RabbitLoader()
        }
        return _instance
    }

    /**
     * Initializes the rabbit driver.
     */
    public void start() {
        // Load the configuration
        connectionConfiguration = new ConnectionConfiguration(grailsApplication.config.rabbitmq?.connection)

        // Connect to the server
        connect()
    }

    /**
     * Starts the individual listeners.
     */
    public void startConsumers() {
        grailsApplication.serviceClasses?.each { GrailsClass service ->
            channels += RabbitConsumer.startConsumer(connection, service)
        }
    }

    /**
     * Reloads the RabbitMQ connection and listeners.
     */
    public void restartConsumers() {
        // Close the existing channels and connection
        stopConsumers()

        // Start the channels again
        startConsumers()
    }

    /**
     * Closes any active channels and the connection to the RabbitMQ server.
     */
    public void stopConsumers() {
        log.info("Closing RabbitMQ channels")
        channels.each { channel ->
            if (channel.isOpen()) {
                channel.close()
            }
        }
        channels = []
    }

    /**
     * Closes all active channels and disconnects from the RabbitMQ server.
     */
    public void stop() {
        // Close all the channels
        stopConsumers()

        // Close the connection
        log.info("Closing connection to the RabbitMQ server")
        connection.close()
        connection = null
    }

    /**
     * Disconnects and completely restarts the connection to the RabbitMQ server.
     */
    public void restart() {
        stop()
        start()
        startConsumers()
    }

    /**
     * Creates the connection to the RabbitMQ server.
     */
    private void connect() {
        // Ensure we don't already have a connection
        if (connection) {
            throw new Exception('will not connect to RabbitMQ; there is already an active connection')
        }

        // Log it
        if (connectionConfiguration.virtualHost) {
            log.info("Connecting to RabbitMQ server at '${connectionConfiguration.host}:${connectionConfiguration.port}' on virtual host '${connectionConfiguration.virtualHost}'.")
        }
        else {
            log.info("Connecting to RabbitMQ server at '${connectionConfiguration.host}:${connectionConfiguration.port}'.")
        }

        // Create the connection
        connection = connectionConfiguration.connection
    }

    /**
     * Closes the connection to the RabbitMQ server when the object is destroyed.
     */
    public void finalize() {
        stop()
    }
}
