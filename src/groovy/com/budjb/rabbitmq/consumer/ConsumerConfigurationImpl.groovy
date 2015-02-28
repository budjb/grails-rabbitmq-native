/*
 * Copyright 2013-2015 Bud Byrd
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
package com.budjb.rabbitmq.consumer

import org.apache.log4j.Logger

class ConsumerConfigurationImpl implements ConsumerConfiguration {
    /**
     * Logger
     */
    private Logger log = Logger.getLogger(ConsumerConfigurationImpl)

    /**
     * Queue to listen on.
     */
    String queue

    /**
     * Exchange to subscribe to.
     */
    String exchange

    /**
     * Consumer binding.
     */
    Object binding

    /**
     * Headers consume binding requirement.
     */
    String match

    /**
     * Number of concurrent consumers.
     */
    int consumers = 1

    /**
     * Whether to mark the consumer as transacted.
     */
    boolean transacted = false

    /**
     * Whether the consumer should auto acknowledge.
     */
    AutoAck autoAck = AutoAck.POST

    /**
     * Whether to attempt conversion of incoming messages.
     * This also depends on the appropriate handler signature being present.
     */
    MessageConvertMethod convert = MessageConvertMethod.ALWAYS

    /**
     * Whether to retry the message on failure.
     */
    boolean retry = false

    /**
     * Number of messages that should be pre-fetched from the queue.
     */
    int prefetchCount = 1

    /**
     * Name of the connection that should be used to consume from.
     */
    String connection

    /**
     * Empty constructor.
     */
    ConsumerConfigurationImpl() {}

    /**
     * Constructor that parses the options defined in the service consumer.
     *
     * @param options
     */
    ConsumerConfigurationImpl(Map options) {
        if (!options == null) {
            throw new NullPointerException("consumer configuration options must not be null")
        }

        setQueue(parseConfigOption(String, queue, options['queue']))
        setExchange(parseConfigOption(String, exchange, options['exchange']))
        setBinding(parseConfigOption(Object, binding, options['binding']))
        setMatch(parseConfigOption(String, match, options['match']))
        setConsumers(parseConfigOption(Integer, consumers, options['consumers']))
        setAutoAck(parseConfigOption(AutoAck, autoAck, options['autoAck']))
        setConvert(parseConfigOption(MessageConvertMethod, convert, options['convert']))
        setRetry(parseConfigOption(Boolean, retry, options['retry']))
        setPrefetchCount(parseConfigOption(Integer, prefetchCount, options['prefetchCount']))
        setConnection(parseConfigOption(String, connection, options['connection']))

        // This is intentionally last
        setTransacted(parseConfigOption(Boolean, transacted, options['transacted']))
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
     * Sets whether the consumer should auto acknowledge.
     */
    @Override
    void setAutoAck(AutoAck autoAck) {
        this.autoAck = autoAck

        if (autoAck != AutoAck.POST) {
            setTransacted(false)
        }
    }

    /**
     * Sets whether to mark the consumer as transacted.
     */
    @Override
    void setTransacted(boolean transacted) {
        this.transacted = transacted

        if (transacted == true) {
            setAutoAck(AutoAck.POST)
        }
    }

    /**
     * Returns whether the configuration is valid.
     *
     * @return
     */
    @Override
    boolean isValid() {
        boolean valid = true

        if (!queue && !exchange) {
            log.warn("consumer is not valid because it has no queue nor exchange defined")
            valid = false
        }

        if (queue && exchange) {
            log.warn("consumer is not valid because is has both a queue and an exchange defined")
            valid = false
        }

        if (binding instanceof Map && !(match in ["any", "all"])) {
            log.warn("match must be either 'any' or 'all'")
            valid = false
        }

        return valid
    }
}
