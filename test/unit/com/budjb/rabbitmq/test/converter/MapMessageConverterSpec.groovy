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

import com.budjb.rabbitmq.converter.MapMessageConverter
import spock.lang.Specification

class MapMessageConverterSpec extends Specification {
    MapMessageConverter messageConverter

    def setup() {
        messageConverter = new MapMessageConverter()
    }

    def 'Ensure the converter reports that it can convert from a map'() {
        messageConverter.canConvertFrom() == true
    }

    def 'Ensure the converter reports that it can convert to a map'() {
        messageConverter.canConvertTo() == true
    }

    def 'Validate conversion from a byte array'() {
        setup:
        byte[] source = [123, 34, 102, 111, 111, 34, 58, 34, 98, 97, 114, 34, 125] as byte[]

        when:
        Map converted = messageConverter.convertTo(source)

        then:
        converted == ["foo": "bar"]
    }

    def 'Validate conversion to a byte array'() {
        setup:
        Map list = ["foo": "bar"]

        when:
        byte[] converted = messageConverter.convertFrom(list)

        then:
        converted == [123, 34, 102, 111, 111, 34, 58, 34, 98, 97, 114, 34, 125] as byte[]
    }

    def 'Ensure the converter has the correct content type'() {
        messageConverter.getContentType() == 'application/json'
    }
}
