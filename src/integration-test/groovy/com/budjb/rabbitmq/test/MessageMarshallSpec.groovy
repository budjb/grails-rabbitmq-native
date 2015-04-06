/*
 * Copyright 2015 Bud Byrd
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

import grails.test.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired

@Integration
class MessageMarshallSpec extends MessageConsumerIntegrationTest {
    @Autowired
    ReportingConsumer reportingConsumer

    def setup() {
        reportingConsumer.lastMessage = null
    }

    def 'If a String is sent to the consumer, ensure the correct handler is called'() {
        when:
        rabbitMessagePublisher.send {
            routingKey = 'reporting'
            body = "foobar"
        }
        Map received = waitUntilMessageReceived(30000) { reportingConsumer.lastMessage }

        then:
        received != null
        received.type == 'String'
        received.body.getClass() == String
        received.body == 'foobar'
    }

    def 'If an Integer is sent to the consumer, ensure the correct handler is called'() {
        when:
        rabbitMessagePublisher.send {
            routingKey = 'reporting'
            body = 1234
        }
        Map received = waitUntilMessageReceived(30000) { reportingConsumer.lastMessage }

        then:
        received != null
        received.type == 'Integer'
        received.body.getClass() == Integer
        received.body == 1234
    }

    def 'If a List is sent to the consumer, ensure the correct handler is called'() {
        when:
        rabbitMessagePublisher.send {
            routingKey = 'reporting'
            body = ["foo", "bar"]
        }
        Map received = waitUntilMessageReceived(30000) { reportingConsumer.lastMessage }

        then:
        received != null
        received.type == 'List'
        received.body instanceof List
        received.body == ["foo", "bar"]
    }

    def 'If a Map is sent to the consumer, ensure the correct handler is called'() {
        when:
        rabbitMessagePublisher.send {
            routingKey = 'reporting'
            body = ["foo": "bar"]
        }
        Map received = waitUntilMessageReceived(30000) { reportingConsumer.lastMessage }

        then:
        received != null
        received.type == 'Map'
        received.body instanceof Map
        received.body == ["foo": "bar"]
    }

    def 'Ensure RPC calls marshall Strings correctly'() {
        when:
        String response = rabbitMessagePublisher.rpc {
            routingKey = 'reporting'
            body = 'foobar'
        }

        then:
        response == 'foobar'
    }

    def 'Ensure RPC calls marshall Integers correctly'() {
        when:
        Integer response = rabbitMessagePublisher.rpc {
            routingKey = 'reporting'
            body = 1234
        }

        then:
        response == 1234
    }

    def 'Ensure RPC calls marshall Lists correctly'() {
        when:
        List response = rabbitMessagePublisher.rpc {
            routingKey = 'reporting'
            body = ["foo", "bar"]
        }

        then:
        response == ["foo", "bar"]
    }

    def 'Ensure RPC calls marshall Maps correctly'() {
        when:
        Map response = rabbitMessagePublisher.rpc {
            routingKey = 'reporting'
            body = ["foo": "bar"]
        }

        then:
        response == ["foo": "bar"]
    }
}
