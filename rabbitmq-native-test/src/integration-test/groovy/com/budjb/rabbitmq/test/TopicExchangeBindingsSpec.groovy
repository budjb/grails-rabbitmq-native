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
package com.budjb.rabbitmq.test

import com.budjb.rabbitmq.test.plugin.ExchangeBindingTopicConsumer
import grails.test.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired

import java.util.concurrent.TimeoutException

@Integration
class TopicExchangeBindingsSpec extends MessageConsumerIntegrationTest {
    @Autowired
    AllTopicConsumer allTopicConsumer

    @Autowired
    SubsetTopicConsumer subsetTopicConsumer

    @Autowired
    SpecificTopicConsumer specificTopicConsumer

    @Autowired
    ExchangeBindingTopicConsumer exchangeBindingTopicConsumer

    def setup() {
        allTopicConsumer.lastMessage = null
        subsetTopicConsumer.lastMessage = null
        specificTopicConsumer.lastMessage = null
        exchangeBindingTopicConsumer.lastMessage = null
    }

    def 'Test topic exchange bindings work by sending a partial-matching topic'() {
        setup:
        rabbitMessagePublisher.send {
            exchange = 'topic-exchange'
            routingKey = 'com.budjb.exchange'
            body = 'test'
        }

        when:
        waitUntilMessageReceived(30000) { allTopicConsumer.lastMessage }

        then:
        allTopicConsumer.lastMessage != null

        when:
        waitUntilMessageReceived(5000) { subsetTopicConsumer.lastMessage }

        then:
        subsetTopicConsumer.lastMessage != null

        when:
        waitUntilMessageReceived(5000) { specificTopicConsumer.lastMessage }

        then:
        thrown TimeoutException

        when:
        waitUntilMessageReceived(5000) { exchangeBindingTopicConsumer.lastMessage }

        then:
        thrown TimeoutException
    }

    def 'Test topic bindings work by sending an exact-matching topic'() {
        setup:
        rabbitMessagePublisher.send {
            exchange = 'topic-exchange'
            routingKey = 'com.budjb.exchange.queue'
            body = 'test'
        }

        when:
        waitUntilMessageReceived(30000) { allTopicConsumer.lastMessage }

        then:
        allTopicConsumer.lastMessage != null

        when:
        waitUntilMessageReceived(5000) { subsetTopicConsumer.lastMessage }

        then:
        subsetTopicConsumer.lastMessage != null

        when:
        waitUntilMessageReceived(5000) { specificTopicConsumer.lastMessage }

        then:
        thrown TimeoutException

        when:
        waitUntilMessageReceived(5000) { exchangeBindingTopicConsumer.lastMessage }

        then:
        exchangeBindingTopicConsumer.lastMessage != null
    }
}
