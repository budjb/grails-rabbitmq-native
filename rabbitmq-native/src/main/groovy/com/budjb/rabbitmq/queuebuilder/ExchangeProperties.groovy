/*
 * Copyright 2013-2017 Bud Byrd
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
package com.budjb.rabbitmq.queuebuilder

import com.budjb.rabbitmq.exception.InvalidConfigurationException

class ExchangeProperties implements ConfigurationProperties {
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
     * Name of exchange to bind to
     */
    List<ExchangeBinding> exchangeBindings = []

    /**
     * Constructor.
     *
     * @param name
     * @param configuration
     */
    ExchangeProperties(Map configuration) {
        name = parseConfigOption(String, configuration.name)
        arguments = parseConfigOption(Map, configuration.arguments, arguments)
        autoDelete = parseConfigOption(Boolean, configuration.autoDelete, autoDelete)
        durable = parseConfigOption(Boolean, configuration.durable, durable)
        type = ExchangeType.lookup(parseConfigOption(String, configuration['type']))
        connection = parseConfigOption(String, configuration.connection, connection)

        /*
        Handle exchange binding to another exchange
        Configuration should be a list of maps
         [
         as : <source|destination> (default is destination)
         exchange : <exchange to bind to>
         binding : <binding key to use>
          ]
         */
        if (configuration.exchangeBindings) {
            if (!(configuration.exchangeBindings instanceof Collection)) {
                throw new IllegalArgumentException("Exchange bindings configuration must be a list of maps")
            }

            configuration.exchangeBindings.each { bindingMap ->
                if (!(bindingMap instanceof Map)) {
                    throw new IllegalArgumentException("Exchange binding configuration must be a list of maps")
                }
                String exc = parseConfigOption(String, bindingMap.exchange)
                String binding = parseConfigOption(String, bindingMap.binding)

                switch (parseConfigOption(String, bindingMap.as)) {
                    case 'source':
                        exchangeBindings += new ExchangeBinding(name, exc, binding)
                        break
                    case 'destination': default:
                        exchangeBindings += new ExchangeBinding(exc, name, binding)
                        break
                }
            }
        }
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

    class ExchangeBinding {
        String source
        String destination
        String binding

        ExchangeBinding(String source, String destination, String binding) {
            this.source = source
            this.destination = destination
            this.binding = binding
        }

        String toString() {
            "$source to $destination: $binding"
        }
    }
}
