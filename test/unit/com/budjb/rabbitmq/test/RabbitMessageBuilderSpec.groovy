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

import com.budjb.rabbitmq.RabbitMessageBuilder
import com.budjb.rabbitmq.RabbitMessageProperties
import com.budjb.rabbitmq.RabbitMessagePublisher
import com.rabbitmq.client.Channel
import spock.lang.Specification

class RabbitMessageBuilderSpec extends Specification {
    RabbitMessagePublisher rabbitMessagePublisher

    def setup() {
        rabbitMessagePublisher = Mock(RabbitMessagePublisher)
    }

    def 'Ensure send() is proxied to the publisher'() {
        setup:
        RabbitMessageBuilder rabbitMessageBuilder = new RabbitMessageBuilder()
        rabbitMessageBuilder.setRabbitMessagePublisher(rabbitMessagePublisher)

        when:
        rabbitMessageBuilder.send()

        then:
        1 * rabbitMessagePublisher.send(_)
    }

    def 'Ensure send(String, Object) is proxied to the publisher'() {
        setup:
        RabbitMessageBuilder rabbitMessageBuilder = new RabbitMessageBuilder()
        rabbitMessageBuilder.setRabbitMessagePublisher(rabbitMessagePublisher)

        when:
        rabbitMessageBuilder.send('test-routing-key', 'foobar')

        then:
        1 * rabbitMessagePublisher.send(_)
        rabbitMessageBuilder.routingKey == 'test-routing-key'
        rabbitMessageBuilder.body == 'foobar'
    }

    def 'Ensure send(String, String, Object) is proxied to the publisher'() {
        setup:
        RabbitMessageBuilder rabbitMessageBuilder = new RabbitMessageBuilder()
        rabbitMessageBuilder.setRabbitMessagePublisher(rabbitMessagePublisher)

        when:
        rabbitMessageBuilder.send('test-exchange', 'test-routing-key', 'foobar')

        then:
        1 * rabbitMessagePublisher.send(_)
        rabbitMessageBuilder.exchange == 'test-exchange'
        rabbitMessageBuilder.routingKey == 'test-routing-key'
        rabbitMessageBuilder.body == 'foobar'
    }

    def 'Ensure send(Closure) is proxied to the publisher'() {
        setup:
        RabbitMessageBuilder rabbitMessageBuilder = new RabbitMessageBuilder()
        rabbitMessageBuilder.setRabbitMessagePublisher(rabbitMessagePublisher)

        when:
        rabbitMessageBuilder.send {
            exchange = 'test-exchange'
            routingKey = 'test-routing-key'
            body = 'foobar'
        }

        then:
        1 * rabbitMessagePublisher.send(_)
        rabbitMessageBuilder.exchange == 'test-exchange'
        rabbitMessageBuilder.routingKey == 'test-routing-key'
        rabbitMessageBuilder.body == 'foobar'
    }

    def 'Ensure rpc() is proxied to the publisher'() {
        setup:
        RabbitMessageBuilder rabbitMessageBuilder = new RabbitMessageBuilder()
        rabbitMessageBuilder.setRabbitMessagePublisher(rabbitMessagePublisher)

        when:
        rabbitMessageBuilder.rpc()

        then:
        1 * rabbitMessagePublisher.rpc(_)
    }

    def 'Ensure rpc(String, Object) is proxied to the publisher'() {
        setup:
        RabbitMessageBuilder rabbitMessageBuilder = new RabbitMessageBuilder()
        rabbitMessageBuilder.setRabbitMessagePublisher(rabbitMessagePublisher)

        when:
        rabbitMessageBuilder.rpc('test-routing-key', 'foobar')

        then:
        1 * rabbitMessagePublisher.rpc(_)
        rabbitMessageBuilder.routingKey == 'test-routing-key'
        rabbitMessageBuilder.body == 'foobar'
    }

    def 'Ensure rpc(String, String, Object) is proxied to the publisher'() {
        setup:
        RabbitMessageBuilder rabbitMessageBuilder = new RabbitMessageBuilder()
        rabbitMessageBuilder.setRabbitMessagePublisher(rabbitMessagePublisher)

        when:
        rabbitMessageBuilder.rpc('test-exchange', 'test-routing-key', 'foobar')

        then:
        1 * rabbitMessagePublisher.rpc(_)
        rabbitMessageBuilder.exchange == 'test-exchange'
        rabbitMessageBuilder.routingKey == 'test-routing-key'
        rabbitMessageBuilder.body == 'foobar'
    }

    def 'Ensure rpc(Closure) is proxied to the publisher'() {
        setup:
        RabbitMessageBuilder rabbitMessageBuilder = new RabbitMessageBuilder()
        rabbitMessageBuilder.setRabbitMessagePublisher(rabbitMessagePublisher)

        when:
        rabbitMessageBuilder.rpc('test-exchange', 'test-routing-key', 'foobar')

        then:
        1 * rabbitMessagePublisher.rpc(_)
        rabbitMessageBuilder.exchange == 'test-exchange'
        rabbitMessageBuilder.routingKey == 'test-routing-key'
        rabbitMessageBuilder.body == 'foobar'
    }

    def 'Ensure rpc(String, String, Object) ix proxied to the publisher'() {
        setup:
        RabbitMessageBuilder rabbitMessageBuilder = new RabbitMessageBuilder()
        rabbitMessageBuilder.setRabbitMessagePublisher(rabbitMessagePublisher)

        when:
        rabbitMessageBuilder.rpc {
            exchange = 'test-exchange'
            routingKey = 'test-routing-key'
            body = 'foobar'
        }

        then:
        1 * rabbitMessagePublisher.rpc(_)
        rabbitMessageBuilder.exchange == 'test-exchange'
        rabbitMessageBuilder.routingKey == 'test-routing-key'
        rabbitMessageBuilder.body == 'foobar'
    }

    def 'Ensure the proper properties object is built when properties are not default'() {
        setup:
        Channel channel = Mock(Channel)

        RabbitMessageBuilder messageBuilder = new RabbitMessageBuilder()
        messageBuilder.appId = 'test-appId'
        messageBuilder.autoConvert = false
        messageBuilder.body = 'test-body'
        messageBuilder.channel = channel
        messageBuilder.connection = 'test-connection'
        messageBuilder.contentEncoding = 'test-encoding'
        messageBuilder.contentType = 'text/plain'
        messageBuilder.correlationId = 'test-correlationId'
        messageBuilder.deliveryMode = 1
        messageBuilder.exchange = 'test-exchange'
        messageBuilder.expiration = 'test-expiration'
        messageBuilder.headers = ['foo': 'bar']
        messageBuilder.messageId = 'test-messageId'
        messageBuilder.priority = 3
        messageBuilder.replyTo = 'test-replyTo'
        messageBuilder.routingKey = 'test-routingKey'
        messageBuilder.timeout = 10000
        messageBuilder.type = 'test-type'
        messageBuilder.userId = 'test-userId'

        when:
        RabbitMessageProperties properties = messageBuilder.buildMessageProperties()

        then:
        properties.appId == 'test-appId'
        properties.autoConvert == false
        properties.body == 'test-body'
        properties.channel == channel
        properties.connection == 'test-connection'
        properties.contentEncoding == 'test-encoding'
        properties.contentType == 'text/plain'
        properties.correlationId == 'test-correlationId'
        properties.deliveryMode == 1
        properties.exchange == 'test-exchange'
        properties.expiration == 'test-expiration'
        properties.headers == ['foo': 'bar']
        properties.messageId == 'test-messageId'
        properties.priority == 3
        properties.replyTo == 'test-replyTo'
        properties.routingKey == 'test-routingKey'
        properties.timeout == 10000
        properties.type == 'test-type'
        properties.userId == 'test-userId'
    }

    def 'Ensure the proper properties object is built when properties are default'() {
        setup:
        Channel channel = Mock(Channel)

        RabbitMessageBuilder messageBuilder = new RabbitMessageBuilder()

        when:
        RabbitMessageProperties properties = messageBuilder.buildMessageProperties()

        then:
        properties.appId == null
        properties.autoConvert == true
        properties.body == null
        properties.channel == null
        properties.connection == null
        properties.contentEncoding == null
        properties.contentType == null
        properties.correlationId == null
        properties.deliveryMode == 0
        properties.exchange == ''
        properties.expiration == null
        properties.headers == [:]
        properties.messageId == null
        properties.priority == 0
        properties.replyTo == null
        properties.routingKey == ''
        properties.timeout == 5000
        properties.type == null
        properties.userId == null
    }
}
