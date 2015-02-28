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
package com.budjb.rabbitmq.test.helper

import com.budjb.rabbitmq.publisher.RabbitMessagePublisher
import grails.plugin.spock.IntegrationSpec

import java.util.concurrent.TimeoutException

class MessageConsumerIntegrationTest extends IntegrationSpec {
    /**
     * Rabbit message publisher bean.
     */
    RabbitMessagePublisher rabbitMessagePublisher

    /**
     * Waits for a consumer to receive a message.
     *
     * @param timeout Time, in milliseconds, to wait for the message to be received.
     * @param closure Close that contains the logic to determine if the consumer has received the message.
     * @return
     */
    def waitUntilMessageReceived(int timeout, Closure closure) throws TimeoutException {
        long start = System.currentTimeMillis()
        while (System.currentTimeMillis() < start + timeout) {
            def value = closure()
            if (value != null) {
                return value
            }
            sleep(1000)
        }
        throw new TimeoutException("no message received in ${timeout} milliseconds")
    }
}
