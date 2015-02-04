/*
 * Copyright 2013-2014 Bud Byrd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.budjb.rabbitmq

class ConsumerConfiguration {
    /**
     * Queue to listen on.
     */
    protected String queue = null

    /**
     * Exchange to subscribe to.
     */
    protected String exchange = null

    /**
     * Consumer binding.
     */
    protected Object binding = null

    /**
     * Headers consume binding requirement.
     */
    protected String match

    /**
     * Number of concurrent consumers.
     */
    protected int consumers = 1

    /**
     * Whether to mark the consumer as transacted.
     */
    protected boolean transacted = false

    /**
     * Whether the consumer should auto acknowledge.
     */
    protected AutoAck autoAck = AutoAck.POST

    /**
     * Whether to attempt conversion of incoming messages.
     * This also depends on the appropriate handler signature being present.
     */
    protected MessageConvertMethod convert = MessageConvertMethod.ALWAYS

    /**
     * Whether to retry the message on failure.
     */
    protected boolean retry = false

    /**
     * Number of messages that should be pre-fetched from the queue.
     */
    protected int prefetchCount = 1

    /**
     * Name of the connection that should be used to consume from.
     */
    protected String connection = null

    /**
     * Constructor that parses the options defined in the service consumer.
     *
     * @param options
     */
    public ConsumerConfiguration(Map options) {
        queue         = parseConfigOption(String, queue, options['queue'])
        exchange      = parseConfigOption(String, exchange, options['exchange'])
        binding       = parseConfigOption(Object, binding, options['binding'])
        match         = parseConfigOption(String, match, options['match'])
        consumers     = parseConfigOption(Integer, consumers, options['consumers'])
        transacted    = parseConfigOption(Boolean, transacted, options['transacted'])
        autoAck       = parseConfigOption(AutoAck, autoAck, options['autoAck'])
        convert       = parseConfigOption(MessageConvertMethod, convert, options['convert'])
        retry         = parseConfigOption(Boolean, retry, options['retry'])
        prefetchCount = parseConfigOption(Integer, prefetchCount, options['prefetchCount'])
        connection    = parseConfigOption(String, connection, options['connection'])

        if (transacted) {
            autoAck = AutoAck.POST
        }
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

    /**
     * Returns whether the consumer should auto acknowledge.
     */
    public AutoAck getAutoAck() {
        return autoAck
    }

    /**
     * Sets whether the consumer should auto acknowledge.
     */
    public void setAutoAck(AutoAck autoAck) {
        this.autoAck = autoAck
    }

    /**
     * Returns the consumer binding.
     */
    public Object getBinding() {
        return binding
    }

    /**
     * Sets the consumer binding.
     */
    public void setBinding(Object binding) {
        this.binding = binding
    }

    /**
     * Returns the name of the connection that should be used to consume from.
     */
    public String getConnection() {
        return connection
    }

    /**
     * Sets the name of the connection that should be used to consume from.
     */
    public void setConnection(String connection) {
        this.connection = connection
    }

    /**
     * Returns the number of concurrent consumers.
     */
    public int getConsumers() {
        return consumers
    }

    /**
     * Sets the number of concurrent consumers.
     */
    public void setConsumers(int consumers) {
        this.consumers = consumers
    }

    /**
     * Returns whether to attempt conversion of incoming messages.
     * This also depends on the appropriate handler signature being present.
     */
    public MessageConvertMethod getConvert() {
        return convert
    }

    /**
     * Sets whether to attempt conversion of incoming messages.
     * This also depends on the appropriate handler signature being present.
     */
    public void setConvert(MessageConvertMethod convert) {
        this.convert = convert
    }

    /**
     * Returns the exchange to subscribe to.
     */
    public String getExchange() {
        return exchange
    }

    /**
     * Sets the exchange to subscribe to.
     */
    public void setExchange(String exchange) {
        this.exchange = exchange
    }

    /**
     * Returns the headers consume binding requirement.
     */
    public String getMatch() {
        return match
    }

    /**
     * Sets the headers consume binding requirement.
     */
    public void setMatch(String match) {
        this.match = match
    }

    /**
     * Returns the number of messages that should be pre-fetched from the queue.
     */
    public int getPrefetchCount() {
        return prefetchCount
    }

    /**
     * Sets the number of messages that should be pre-fetched from the queue.
     */
    public void setPrefetchCount(int prefetchCount) {
        this.prefetchCount = prefetchCount
    }

    /**
     * Returns the queue to listen on.
     */
    public String getQueue() {
        return queue
    }

    /**
     * Sets the queue to listen on.
     */
    public void setQueue(String queue) {
        this.queue = queue
    }

    /**
     * Returns whether to retry the message on failure.
     */
    public boolean getRetry() {
        return retry
    }

    /**
     * Sets whether to retry the message on failure.
     */
    public void setRetry(int retry) {
        this.retry = retry
    }

    /**
     * Returns whether to mark the consumer as transacted.
     */
    public boolean getTransacted() {
        return transacted
    }

    /**
     * Sets whether to mark the consumer as transacted.
     */
    public void setTransacted(boolean transacted) {
        this.transacted = transacted
    }
}
