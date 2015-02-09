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

import org.apache.commons.lang.NullArgumentException

import com.budjb.rabbitmq.RabbitContext
import com.budjb.rabbitmq.RabbitContextProxy

import spock.lang.Specification

class RabbitContextProxySpec extends Specification {
    RabbitContextProxy rabbitContext
    RabbitContext targetRabbitContext

    def setup() {
        targetRabbitContext = Mock(RabbitContext)

        rabbitContext = new RabbitContextProxy()
        rabbitContext.setTarget(targetRabbitContext)
    }

    def 'If a null rabbit context is set as the target, a NullArgumentException should be thrown'() {
        when:
        rabbitContext.setTarget(null)

        then:
        thrown NullArgumentException
    }

    def 'If a RabbitContextProxy is set as the target, an IllegalArgumentException should be thrown'() {
        when:
        rabbitContext.setTarget(new RabbitContextProxy())

        then:
        thrown IllegalArgumentException
    }

    def 'If spring injects a null rabbit context, an assertion error should occur'() {
        setup:
        rabbitContext = new RabbitContextProxy()

        when:
        rabbitContext.afterPropertiesSet()

        then:
        thrown AssertionError
    }

    def 'Ensure createChannel() is proxied'() {
        when:
        rabbitContext.createChannel()

        then:
        1 * targetRabbitContext.createChannel()
    }

    def 'Ensure createChannel(String) is proxied'() {
        when:
        rabbitContext.createChannel("test")

        then:
        1 * targetRabbitContext.createChannel("test")
    }

    def 'Ensure getConnection() is proxied'() {
        when:
        rabbitContext.getConnection()

        then:
        1 * targetRabbitContext.getConnection()
    }

    def 'Ensure getConnection(String) is proxied'() {
        when:
        rabbitContext.getConnection("test")

        then:
        1 * targetRabbitContext.getConnection("test")
    }

    def 'Ensure load() is proxied'() {
        when:
        rabbitContext.load()

        then:
        1 * targetRabbitContext.load()
    }

    def 'Ensure registerConsumer(Object) is proxied'() {
        when:
        rabbitContext.registerConsumer(null)

        then:
        1 * targetRabbitContext.registerConsumer(null)
    }

    def 'Ensure registerMessageConverter(MessageConverter) is proxied'() {
        when:
        rabbitContext.registerMessageConverter(null)

        then:
        1 * targetRabbitContext.registerMessageConverter(null)
    }

    def 'Ensure restart() is proxied'() {
        when:
        rabbitContext.restart()

        then:
        1 * targetRabbitContext.restart()
    }

    def 'Ensure setApplicationContext(ApplicationContext) is proxied'() {
        when:
        rabbitContext.setApplicationContext(null)

        then:
        1 * targetRabbitContext.setApplicationContext(null)
    }

    def 'Ensure setConnectionManager(ConnectionManager) is proxied'() {
        when:
        rabbitContext.setConnectionManager(null)

        then:
        1 * targetRabbitContext.setConnectionManager(null)
    }

    def 'Ensure setMessageConverterManager(MessageConverterManager) is proxied'() {
        when:
        rabbitContext.setMessageConverterManager(null)

        then:
        1 * targetRabbitContext.setMessageConverterManager(null)
    }

    def 'Ensure setConsumerManager(ConsumerManager) is proxied'() {
        when:
        rabbitContext.setConsumerManager(null)

        then:
        1 * targetRabbitContext.setConsumerManager(null)
    }

    def 'Ensure setQueueBuilder(QueueBuilder) is proxied'() {
        when:
        rabbitContext.setQueueBuilder(null)

        then:
        1 * targetRabbitContext.setQueueBuilder(null)
    }

    def 'Ensure start() is proxied'() {
        when:
        rabbitContext.start()

        then:
        1 * targetRabbitContext.start()
    }

    def 'Ensure start(boolean) is proxied'() {
        when:
        rabbitContext.start(false)

        then:
        1 * targetRabbitContext.start(false)
    }

    def 'Ensure startConsumers() is proxied'() {
        when:
        rabbitContext.startConsumers()

        then:
        1 * targetRabbitContext.startConsumers()
    }

    def 'Ensure stop() is proxied'() {
        when:
        rabbitContext.stop()

        then:
        1 * targetRabbitContext.stop()
    }
}
