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

import com.budjb.rabbitmq.RabbitContext
import com.budjb.rabbitmq.RabbitLifecycleListener
import grails.config.Config
import spock.lang.Specification
import spock.lang.Unroll

class RabbitLifecycleListenerSpec extends Specification {
    RabbitLifecycleListener rabbitLifecycleListener
    RabbitContext rabbitContext
    Config config

    def setup() {
        rabbitContext = Mock(RabbitContext)
        config = Mock(Config)

        rabbitLifecycleListener = new RabbitLifecycleListener()
        rabbitLifecycleListener.rabbitContext = rabbitContext
        rabbitLifecycleListener.configuration = config
    }

    @Unroll
    void 'Validate isEnabled() == #expect (enabled: #enabled)'() {
        setup:
        config.getProperty('rabbitmq.enabled', Boolean, true) >> enabled

        expect:
        rabbitLifecycleListener.isEnabled() == expect

        where:
        enabled || expect
        true    || true
        false   || false

        'true'  || true
        'false' || true
        1       || true
        0       || false
        '1'     || true
        '0'     || true
    }

    void 'Validate doWithApplicationContext() (enabled: true)'() {
        setup:
        config.getProperty('rabbitmq.enabled', Boolean, true) >> true

        when:
        rabbitLifecycleListener.doWithApplicationContext()

        then:
        1 * rabbitContext.load()
        0 * rabbitContext.start()
    }

    void 'Validate doWithApplicationContext() (enabled: false)'() {
        setup:
        config.getProperty('rabbitmq.enabled', Boolean, true) >> false

        when:
        rabbitLifecycleListener.doWithApplicationContext()

        then:
        0 * rabbitContext.load()
        0 * rabbitContext.start()
    }

    void 'Validate onStartup() (enabled: true)'() {
        setup:
        config.getProperty('rabbitmq.enabled', Boolean, true) >> true

        when:
        rabbitLifecycleListener.onStartup([:])

        then:
        0 * rabbitContext.load()
        1 * rabbitContext.start()
    }

    void 'Validate onStartup() (enabled: false)'() {
        setup:
        config.getProperty('rabbitmq.enabled', Boolean, true) >> false

        when:
        rabbitLifecycleListener.onStartup([:])

        then:
        0 * rabbitContext.load()
        0 * rabbitContext.start()
    }
}
