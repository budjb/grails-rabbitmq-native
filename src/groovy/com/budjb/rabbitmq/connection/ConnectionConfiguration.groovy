package com.budjb.rabbitmq.connection

import org.apache.log4j.Logger

import com.rabbitmq.client.ConnectionFactory

class ConnectionConfiguration {
    /**
     * Logger.
     */
    Logger log = Logger.getLogger(ConnectionConfiguration)

    /**
     * RabbitMQ host.
     */
    private String host

    /**
     * RabbitMQ name.
     */
    private String name

    /**
     * Whether the connection is the default connection.
     */
    private boolean isDefault = false

    /**
     * Username.
     */
    private String username

    /**
     * Password.
     */
    private String password

    /**
     * Virtual host.
     */
    private String virtualHost = ConnectionFactory.DEFAULT_VHOST

    /**
     * Whether to automatically reconnect.
     */
    private boolean automaticReconnect = true

    /**
     * Number of concurrent threads (0 is unlimited).
     */
    private int threads = 0

    /**
     * Sets the requested heartbeat delay, in seconds, that the server sends in the connection.tune frame.
     *
     * 5 is the RabbitMQ default. 0 means unlimited.
     */
    private int requestedHeartbeat = ConnectionFactory.DEFAULT_HEARTBEAT

    /**
     * Whether the connection uses SSL.
     */
    private boolean ssl = false

    /**
     * Port to use to connect to the RabbitMQ broker.
     */
    private int port = ConnectionFactory.DEFAULT_AMQP_PORT

    /**
     * Basic constructor.
     */
    public ConnectionConfiguration() { }

    /**
     * Constructor.
     *
     * @param configuration
     */
    public ConnectionConfiguration(Map configuration) {
        // Assign values
        automaticReconnect  = parseConfigOption(Boolean, automaticReconnect, configuration['automaticReconnect'])
        host                = parseConfigOption(String, host, configuration['host'])
        isDefault           = parseConfigOption(Boolean, isDefault, configuration['isDefault'])
        name                = parseConfigOption(String, name, configuration['name'])
        password            = parseConfigOption(String, password, configuration['password'])
        port                = parseConfigOption(Integer, port, configuration['port'])
        requestedHeartbeat  = parseConfigOption(Integer, requestedHeartbeat, configuration['requestedHeartbeat'])
        ssl                 = parseConfigOption(Boolean, ssl, configuration['ssl'])
        threads             = parseConfigOption(Integer, threads, configuration['threads'])
        username            = parseConfigOption(String, username, configuration['username'])
        virtualHost         = parseConfigOption(String, virtualHost, configuration['virtualHost'])

        // Validate the configuration
        validateConfiguration()
    }

    /**
     * Parses a configuration option given a class type, default value, and input value.
     *
     * @param clazz
     * @param defaultValue
     * @param value
     * @return
     */
    private Object parseConfigOption(Class clazz, Object defaultValue, Object value) {
        if (value == null) {
            return defaultValue
        }
        try {
            return value.asType(clazz)
        }
        catch (Exception e) {
            return defaultValue
        }
    }

    /**
     * Validates that we have the minimum of information needed to connect to RabbitMQ.
     */
    public void validateConfiguration() {
        assert host != null, "RabbitMQ connection host configuration is missing"
        assert username != null, "RabbitMQ connection username is missing"
        assert password != null, "RabbitMQ connection password is missing"
        assert !virtualHost.isEmpty(), "RabbitMQ connection virtualHost is missing"
        assert port > 0, "RabbitMQ connection port is missing"
        assert threads >= 0, "RabbitMQ connection threads must be greater than or equal to 0"

        if (!name) {
            name = UUID.randomUUID().toString()
            log.warn("connection to RabbitMQ host '${host}:${port}' on virtual host '${virtualHost}' had no name assigned; assigning name '${name}'")
        }
    }

    /**
     * Returns the RabbitMQ host.
     */
    public String getHost() {
        return host
    }

    /**
     * Sets the RabbitMQ host.
     */
    public void setHost(String host) {
        this.host = host
    }

    /**
     * Returns the name of the connection.
     */
    public String getName() {
        return name
    }

    /**
     * Sets the name of the connection.
     */
    public void setName(String name) {
        this.name = name
    }

    /**
     * Returns whether the connection is the default connection.
     */
    public boolean getIsDefault() {
        return isDefault
    }

    /**
     * Sets whether the connection is the default connection.
     */
    public void setIsDefault(boolean isDefault) {
        this.isDefault = isDefault
    }

    /**
     * Returns the RabbitMQ broker port.
     */
    public int getPort() {
        return port
    }

    /**
     * Sets the RabbitMQ broker port.
     */
    public void setPort(int port) {
        this.port = port
    }

    /**
     * Returns the RabbitMQ username.
     */
    public String getUsername() {
        return username
    }

    /**
     * Sets the RabbitMQ username.
     */
    public void setUsername(String username) {
        this.username = username
    }

    /**
     * Returns the RabbitMQ password.
     */
    public String getPassword() {
        return password
    }

    /**
     * Sets the RabbitMQ password.
     */
    public void setPassword(String password) {
        this.password = password
    }

    /**
     * Returns the RabbitMQ virtual host.
     */
    public String getVirtualHost() {
        return virtualHost
    }

    /**
     * Sets the RabbitMQ virtual host.
     */
    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost
    }

    /**
     * Returns whether the connection will automatically reconnect.
     */
    public boolean getAutomaticReconnect() {
        return automaticReconnect
    }

    /**
     * Sets whether the connection will automatically reconnect.
     */
    public void setAutomaticReconnect(boolean automaticReconnect) {
        this.automaticReconnect = automaticReconnect
    }

    /**
     * Returns the maximum number of concurrent consumer threads that are processed.
     *
     * 5 is the RabbitMQ default. 0 means unlimited.
     */
    public int getThreads() {
        return threads
    }

    /**
     * Sets the maximum number of concurrent consumer threads that are processed.
     *
     * 5 is the RabbitMQ default. 0 means unlimited.
     */
    public void setThreads(int threads) {
        this.threads = threads
    }

    /**
     * Returns the requested heartbeat delay, in seconds, that the server sends in the connection.tune frame.
     *
     * If set to 0, heartbeats are disabled.
     */
    public int getRequestedHeartbeat() {
        return requestedHeartbeat
    }

    /**
     * Sets the requested heartbeat delay, in seconds, that the server sends in the connection.tune frame.
     *
     * If set to 0, heartbeats are disabled.
     */
    public void setRequestedHeartbeat(int requestedHeartbeat) {
        this.requestedHeartbeat = requestedHeartbeat
    }

    /**
     * Returns whether to use SSL.
     */
    public boolean getSsl() {
        return ssl
    }

    /**
     * Sets whether to use SSL.
     */
    public void setSsl(boolean ssl) {
        this.ssl = ssl
    }
}
