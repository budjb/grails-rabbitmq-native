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
package com.budjb.rabbitmq.test.queuebuilder

import com.budjb.rabbitmq.exception.InvalidConfigurationException
import com.budjb.rabbitmq.queuebuilder.ExchangeProperties
import com.budjb.rabbitmq.queuebuilder.ExchangeType
import spock.lang.*

class ExchangePropertiesSpec extends Specification {

    ExchangeProperties exchangeProperties

    void 'Validate new ExchangeProperties(Map) (no Exchange Bindings)'() {
        setup:
        Map configuration = [
            name: 'name',
            arguments: [
                'x-message-ttl': 400,
            ],
            autoDelete: true,
            durable: true,
            type: ExchangeType.DIRECT.toString(Locale.US),
            connection: 'connection',
        ]

        when:
        exchangeProperties = new ExchangeProperties(configuration)
        exchangeProperties.validate()

        then:
        notThrown Exception
        exchangeProperties.name == configuration.name
        exchangeProperties.arguments.size() == configuration.arguments.size()
        exchangeProperties.arguments.'x-message-ttl' == configuration.arguments.'x-message-ttl'
        exchangeProperties.autoDelete == configuration.autoDelete
        exchangeProperties.durable == configuration.durable
        exchangeProperties.type == ExchangeType.DIRECT
        exchangeProperties.connection == configuration.connection
        !exchangeProperties.exchangeBindings.size()
    }

    void 'Validate new ExchangeProperties(Map) (with Exchange Bindings)'() {
        setup:
        Map configuration = [
            name: 'name',
            arguments: [
                'x-message-ttl': 400,
            ],
            autoDelete: true,
            durable: true,
            type: ExchangeType.DIRECT.toString(Locale.US),
            connection: 'connection',
            exchangeBindings: [
                [
                    exchange: 'exchange1',
                    binding: 'exchange2',
                    as: 'source'
                ],
            ]
        ]

        when:
        exchangeProperties = new ExchangeProperties(configuration)
        exchangeProperties.validate()

        then:
        notThrown Exception
        exchangeProperties.name == configuration.name
        exchangeProperties.arguments.size() == configuration.arguments.size()
        exchangeProperties.arguments.'x-message-ttl' == configuration.arguments.'x-message-ttl'
        exchangeProperties.autoDelete == configuration.autoDelete
        exchangeProperties.durable == configuration.durable
        exchangeProperties.type == ExchangeType.DIRECT
        exchangeProperties.connection == configuration.connection
        exchangeProperties.exchangeBindings.size() == 1
    }

    void 'Validate new ExchangeProperties(Map) (with Exchange Bindings; Malformed Configuration)'() {
        setup:
        Map configuration = [
            name: 'name',
            arguments: [
                'x-message-ttl': 400,
            ],
            autoDelete: true,
            durable: true,
            type: ExchangeType.DIRECT.toString(Locale.US),
            connection: 'connection',
        ]

        when:
        configuration.exchangeBindings = true
        exchangeProperties = new ExchangeProperties(configuration)

        then:
        thrown IllegalArgumentException

        when:
        configuration.exchangeBindings = [true]
        exchangeProperties = new ExchangeProperties(configuration)

        then:
        thrown IllegalArgumentException
    }

    void 'Validate validate() throws InvalidConfigurationException'() {
        when:
        exchangeProperties = new ExchangeProperties([:])
        exchangeProperties.validate()

        then:
        thrown InvalidConfigurationException

        when:
        exchangeProperties = new ExchangeProperties(name: 'name')
        exchangeProperties.validate()

        then:
        thrown InvalidConfigurationException

        when:
        exchangeProperties = new ExchangeProperties([type: ExchangeType.DIRECT.toString(Locale.US)])
        exchangeProperties.validate()

        then:
        thrown InvalidConfigurationException
    }
}
