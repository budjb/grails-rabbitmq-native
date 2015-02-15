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
package com.budjb.rabbitmq.test.converter

import com.budjb.rabbitmq.converter.IntegerMessageConverter
import spock.lang.Specification

class IntegerMessageConverterSpec extends Specification {
    IntegerMessageConverter messageConverter

    def setup() {
        messageConverter = new IntegerMessageConverter()
    }

    def 'Ensure the converter reports that it can convert from an integer'() {
        messageConverter.canConvertFrom() == true
    }

    def 'Ensure the converter reports that it can convert to an integer'() {
        messageConverter.canConvertTo() == true
    }

    def 'Validate conversion from a byte array'() {
        setup:
        byte[] source = [49, 50, 51, 52] as byte[]

        when:
        int converted = messageConverter.convertTo(source)

        then:
        converted == 1234
    }

    def 'Validate conversion to a byte array'() {
        setup:
        int value = 1234

        when:
        byte[] converted = messageConverter.convertFrom(value)

        then:
        converted == [49, 50, 51, 52] as byte[]
    }

    def 'Ensure the converter has no content type'() {
        messageConverter.getContentType() == null
    }
}
