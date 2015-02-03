package com.budjb.rabbitmq

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
    @Deprecated
    public void registerMessageConverter(MessageConverter converter)

    /**
     * Returns a list of all registered message converters.
     *
     * @return
     */
    @Deprecated
    public List<MessageConverter> getMessageConverters()

    /**
     * Registers a consumer.
     *
     * @param candidate
     */
    public void registerConsumer(DefaultGrailsMessageConsumerClass candidate)

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
}
