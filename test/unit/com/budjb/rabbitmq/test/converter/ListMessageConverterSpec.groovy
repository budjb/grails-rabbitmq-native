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

import com.budjb.rabbitmq.converter.ListMessageConverter
import spock.lang.Specification

class ListMessageConverterSpec extends Specification {
    ListMessageConverter messageConverter

    def setup() {
        messageConverter = new ListMessageConverter()
    }

    def 'Ensure the converter reports that it can convert from a list'() {
        messageConverter.canConvertFrom() == true
    }

    def 'Ensure the converter reports that it can convert to a list'() {
        messageConverter.canConvertTo() == true
    }

    def 'Validate conversion from a byte array'() {
        setup:
        byte[] source = [91, 34, 102, 111, 111, 34, 44, 34, 98, 97, 114, 34, 93] as byte[]

        when:
        List converted = messageConverter.convertTo(source)

        then:
        converted == ["foo", "bar"]
    }

    def 'Validate conversion to a byte array'() {
        setup:
        List list = ["foo", "bar"]

        when:
        byte[] converted = messageConverter.convertFrom(list)

        then:
        converted == [91, 34, 102, 111, 111, 34, 44, 34, 98, 97, 114, 34, 93] as byte[]
    }

    def 'Ensure the converter has the correct content type'() {
        messageConverter.getContentType() == 'application/json'
    }
}
