package com.budjb.rabbitmq.test.queuebuilder

import com.budjb.rabbitmq.exception.InvalidConfigurationException
import com.budjb.rabbitmq.queuebuilder.ExchangeProperties
import com.budjb.rabbitmq.queuebuilder.ExchangeType
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.*

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
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
            type: ExchangeType.DIRECT.toString(),
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
            type: ExchangeType.DIRECT.toString(),
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
            type: ExchangeType.DIRECT.toString(),
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
        exchangeProperties = new ExchangeProperties([type: ExchangeType.DIRECT.toString()])
        exchangeProperties.validate()

        then:
        thrown InvalidConfigurationException
    }
}
