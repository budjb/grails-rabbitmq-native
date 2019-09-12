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
package com.budjb.rabbitmq.test.queuebuilder

import com.budjb.rabbitmq.exception.InvalidConfigurationException
import com.budjb.rabbitmq.queuebuilder.QueueProperties
import spock.lang.Specification

class QueuePropertiesSpec extends Specification {

    QueueProperties properties

    void 'Validate validate()'() {
        when:
        properties = new QueueProperties(name: 'foo')
        properties.validate()

        then:
        notThrown Exception

        when:
        properties = new QueueProperties([:])
        properties.validate()

        then:
        thrown InvalidConfigurationException

        when:
        properties = new QueueProperties([name: 'foo', binding: [:]])
        properties.validate()

        then:
        thrown InvalidConfigurationException
    }
}
