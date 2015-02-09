/*
 * Copyright 2015 Bud Byrd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.budjb.rabbitmq.test.connection

import com.budjb.rabbitmq.*
import com.budjb.rabbitmq.consumer.ConsumerConfiguration
import com.budjb.rabbitmq.converter.MessageConvertMethod

import groovy.util.ConfigObject
import spock.lang.Specification

class ConsumerConfigurationSpec extends Specification {
    def 'Test default settings'() {
        when:
        ConsumerConfiguration configuration = new ConsumerConfiguration([:])

        then:
        configuration.queue == null
        configuration.exchange == null
        configuration.binding == null
        configuration.match == null
        configuration.consumers == 1
        configuration.transacted == false
        configuration.autoAck == AutoAck.POST
        configuration.convert == MessageConvertMethod.ALWAYS
        configuration.retry == false
        configuration.prefetchCount == 1
        configuration.connection == null
    }

    def 'Test non-default settings'() {
        setup:
        Map configurationOptions = [
            queue: 'test-queue',
            exchange: 'test-exchange',
            binding: 'test-binding.#',
            match: 'all',
            consumers: 10,
            transacted: true,
            autoAck: AutoAck.MANUAL,
            convert: MessageConvertMethod.DISABLED,
            retry: true,
            prefetchCount: 10,
            connection: 'non-default-connection'
        ]

        when:
        ConsumerConfiguration configuration = new ConsumerConfiguration(configurationOptions)

        then:
        configuration.queue == 'test-queue'
        configuration.exchange == 'test-exchange'
        configuration.binding == 'test-binding.#'
        configuration.match == 'all'
        configuration.consumers == 10
        configuration.transacted == true
        configuration.autoAck == AutoAck.POST // this is overridden because of transacted
        configuration.convert == MessageConvertMethod.DISABLED
        configuration.retry == true
        configuration.prefetchCount == 10
        configuration.connection == 'non-default-connection'
    }

    def 'Test grails configuration'() {
        setup:
        ConfigObject configurationOptions = new ConfigObject()
        configurationOptions.putAll([
            queue: 'test-queue',
            exchange: 'test-exchange',
            binding: 'test-binding.#',
            match: 'all',
            consumers: 10,
            transacted: true,
            autoAck: AutoAck.MANUAL,
            convert: MessageConvertMethod.DISABLED,
            retry: true,
            prefetchCount: 10,
            connection: 'non-default-connection'
        ])

        when:
        ConsumerConfiguration configuration = new ConsumerConfiguration(configurationOptions)

        then:
        configuration.queue == 'test-queue'
        configuration.exchange == 'test-exchange'
        configuration.binding == 'test-binding.#'
        configuration.match == 'all'
        configuration.consumers == 10
        configuration.transacted == true
        configuration.autoAck == AutoAck.POST // this is overridden because of transacted
        configuration.convert == MessageConvertMethod.DISABLED
        configuration.retry == true
        configuration.prefetchCount == 10
        configuration.connection == 'non-default-connection'
    }
}
