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
package com.budjb.rabbitmq.test

import java.util.concurrent.TimeoutException

class TopicExchangeSpec extends MessageConsumerIntegrationTest {
    AllTopicConsumer allTopicConsumer
    SubsetTopicConsumer subsetTopicConsumer
    SpecificTopicConsumer specificTopicConsumer

    def setup() {
        allTopicConsumer.lastMessage = null
        subsetTopicConsumer.lastMessage = null
        specificTopicConsumer.lastMessage = null
    }

    def 'Test topic bindings work by sending a non-matching topic'() {
        setup:
        rabbitMessagePublisher.send {
            exchange = 'topic-exchange'
            routingKey = 'com.example'
            body = 'test'
        }

        when:
        waitUntilMessageReceived(30000) { allTopicConsumer.lastMessage }

        then:
        allTopicConsumer.lastMessage != null

        when:
        waitUntilMessageReceived(1000) { subsetTopicConsumer.lastMessage }

        then:
        thrown TimeoutException

        when:
        waitUntilMessageReceived(1000) { specificTopicConsumer.lastMessage }

        then:
        thrown TimeoutException
    }

    def 'Test topic bindings work by sending a partial-matching topic'() {
        setup:
        rabbitMessagePublisher.send {
            exchange = 'topic-exchange'
            routingKey = 'com.budjb.test'
            body = 'test'
        }

        when:
        waitUntilMessageReceived(30000) { allTopicConsumer.lastMessage }

        then:
        allTopicConsumer.lastMessage != null

        when:
        waitUntilMessageReceived(1000) { subsetTopicConsumer.lastMessage }

        then:
        subsetTopicConsumer.lastMessage != null

        when:
        waitUntilMessageReceived(1000) { specificTopicConsumer.lastMessage }

        then:
        thrown TimeoutException
    }

    def 'Test topic bindings work by sending an exact-matching topic'() {
        setup:
        rabbitMessagePublisher.send {
            exchange = 'topic-exchange'
            routingKey = 'com.budjb.rabbitmq'
            body = 'test'
        }

        when:
        waitUntilMessageReceived(30000) { allTopicConsumer.lastMessage }

        then:
        allTopicConsumer.lastMessage != null

        when:
        waitUntilMessageReceived(1000) { subsetTopicConsumer.lastMessage }

        then:
        subsetTopicConsumer.lastMessage != null

        when:
        waitUntilMessageReceived(1000) { specificTopicConsumer.lastMessage }

        then:
        specificTopicConsumer.lastMessage != null
    }
}
