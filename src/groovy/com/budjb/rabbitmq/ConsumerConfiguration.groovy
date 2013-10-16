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
    public String routingKey = ''

    /**
     * Number of concurrent consumers.
     */
    public int consumers = 1

    /**
     * Whether to mark the consumer as transacted.
     */
    public boolean transacted = false

    /**
     * Whether the consumer should auto acknowledge.
     */
    public AutoAck autoAck = AutoAck.POST

    /**
     * Whether to attempt conversion of incoming messages.
     * This also depends on the appropriate handler signature being present.
     */
    public MessageConvertMethod convert = MessageConvertMethod.ALWAYS

    /**
     * Whether to retry the message on failure.
     */
    boolean retry = false

    /**
     * Constructor that parses the options defined in the service consumer.
     *
     * @param options
     */
    public ConsumerConfiguration(Map options) {
        queue =         parseConfigOption(String, queue, options['queue'])
        exchange =      parseConfigOption(String, exchange, options['exchange'])
        routingKey =    parseConfigOption(String, routingKey, options['routingKey'])
        consumers =     parseConfigOption(Integer, consumers, options['consumers'])
        transacted =    parseConfigOption(Boolean, transacted, options['transacted'])
        autoAck =       parseConfigOption(AutoAck, autoAck, options['autoAck'])
        convert =       parseConfigOption(MessageConvertMethod, convert, options['convert'])
        retry =         parseConfigOption(Boolean, retry, options['retry'])
    }

    /**
     * Assigns the option provided by the consumer's config, or returns the default
     * value if the option was not provided or it was unable to be converted to
     * the correct data type.
     *
     * @param var
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
}
