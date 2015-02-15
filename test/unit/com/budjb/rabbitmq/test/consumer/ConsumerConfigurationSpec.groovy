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
package com.budjb.rabbitmq.test.consumer

import com.budjb.rabbitmq.consumer.AutoAck
import com.budjb.rabbitmq.consumer.ConsumerConfiguration
import com.budjb.rabbitmq.consumer.MessageConvertMethod
import spock.lang.Specification

class ConsumerConfigurationSpec extends Specification {
    def 'Validate default values with a basic constructor'() {
        when:
        ConsumerConfiguration configuration = new ConsumerConfiguration()

        then:
        configuration.getAutoAck() == AutoAck.POST
        configuration.getBinding() == null
        configuration.getConnection() == null
        configuration.getConsumers() == 1
        configuration.getConvert() == MessageConvertMethod.ALWAYS
        configuration.getExchange() == null
        configuration.getMatch() == null
        configuration.getPrefetchCount() == 1
        configuration.getQueue() == null
        configuration.getRetry() == false
        configuration.getTransacted() == false
    }

    def 'Validate default values with an empty map constructor'() {
        when:
        ConsumerConfiguration configuration = new ConsumerConfiguration([:])

        then:
        configuration.getAutoAck() == AutoAck.POST
        configuration.getBinding() == null
        configuration.getConnection() == null
        configuration.getConsumers() == 1
        configuration.getConvert() == MessageConvertMethod.ALWAYS
        configuration.getExchange() == null
        configuration.getMatch() == null
        configuration.getPrefetchCount() == 1
        configuration.getQueue() == null
        configuration.getRetry() == false
        configuration.getTransacted() == false
    }

    def 'Validate overridden values with a map constructor'() {
        setup:
        Map properties = [
            autoAck: AutoAck.MANUAL,
            binding: 'test-binding',
            connection: 'test-connection',
            consumers: 5,
            convert: MessageConvertMethod.HEADER,
            exchange: 'test-exchange',
            match: 'all',
            prefetchCount: 5,
            queue: 'test-queue',
            retry: true,
            transacted: true
        ]

        when:
        ConsumerConfiguration configuration = new ConsumerConfiguration(properties)

        then:
        configuration.getAutoAck() == AutoAck.POST
        configuration.getBinding() == 'test-binding'
        configuration.getConnection() == 'test-connection'
        configuration.getConsumers() == 5
        configuration.getConvert() == MessageConvertMethod.HEADER
        configuration.getExchange() == 'test-exchange'
        configuration.getMatch() == 'all'
        configuration.getPrefetchCount() == 5
        configuration.getQueue() == 'test-queue'
        configuration.getRetry() == true
        configuration.getTransacted() == true
    }

    def 'Validate getter/setters'() {
        setup:
        ConsumerConfiguration configuration = new ConsumerConfiguration(properties)

        when:
        configuration.setAutoAck(AutoAck.MANUAL)
        configuration.setBinding('test-binding')
        configuration.setConnection('test-connection')
        configuration.setConsumers(5)
        configuration.setConvert(MessageConvertMethod.HEADER)
        configuration.setExchange('test-exchange')
        configuration.setMatch('all')
        configuration.setPrefetchCount(5)
        configuration.setQueue('test-queue')
        configuration.setRetry(true)
        configuration.setTransacted(true)

        then:
        configuration.getAutoAck() == AutoAck.POST
        configuration.getBinding() == 'test-binding'
        configuration.getConnection() == 'test-connection'
        configuration.getConsumers() == 5
        configuration.getConvert() == MessageConvertMethod.HEADER
        configuration.getExchange() == 'test-exchange'
        configuration.getMatch() == 'all'
        configuration.getPrefetchCount() == 5
        configuration.getQueue() == 'test-queue'
        configuration.getRetry() == true
        configuration.getTransacted() == true
    }

    def 'Validate transacted/autoAck behavior'() {
        setup:
        ConsumerConfiguration configuration = new ConsumerConfiguration()

        when:
        configuration.setTransacted(true)
        configuration.setAutoAck(AutoAck.MANUAL)

        then:
        configuration.getTransacted() == false

        when:
        configuration.setAutoAck(AutoAck.MANUAL)
        configuration.setTransacted(true)

        then:
        configuration.getAutoAck() == AutoAck.POST
    }
}
