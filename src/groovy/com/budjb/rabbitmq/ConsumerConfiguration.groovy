package com.budjb.rabbitmq

class ConsumerConfiguration {
    /**
     * Queue to listen on.
     */
    public String queue = null

    /**
     * Exchange to subscribe to.
     */
    public String exchange = null

    /**
     * Routing key.
     *
     * This should only be used in conjunction with topic exchange subscriptions.
     */
    public String routingKey = null

    /**
     * Number of concurrent listeners.
     */
    public int listeners = 1

    /**
     * Whether to mark the consumer as transacted.
     */
    public boolean transacted = false

    /**
     * Whether the listener should auto acknowledge.
     */
    public boolean autoAck = true

    /**
     * Whether to attempt to convert the message to JSON even if the
     * header type does not indicate it.
     */
    public boolean alwaysConvertJson = false

    /**
     * Whether to attempt conversion of incoming messages.
     * This also depends on the appropriate handler signature being present.
     */
    public boolean convert = true

    /**
     * Constructor that parses the options defined in the service listener.
     *
     * @param options
     */
    public ConsumerConfiguration(Map options) {
        queue = options['queue'] ?: queue
        exchange = options['exchange'] ?: exchange
        routingKey = options['routingKey'] ?: routingKey
        listeners = options['listeners']?.toInteger() ?: listeners
        transacted = options['transacted'] ?: transacted
        autoAck = options['autoAck'] ?: autoAck
        alwaysConvertJson = options['alwaysConvertJson'] ?: alwaysConvertJson
        convert = options['convert'] ?: convert
    }
}
