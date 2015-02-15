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

import com.budjb.rabbitmq.converter.GStringMessageConverter
import spock.lang.Specification

class GStringMessageConverterSpec extends Specification {
    GStringMessageConverter messageConverter

    def setup() {
        messageConverter = new GStringMessageConverter()
    }

    def 'Ensure the converter reports that it can convert from a GString'() {
        messageConverter.canConvertFrom() == true
    }

    def 'Ensure the converter reports that it can NOT convert to a GString'() {
        messageConverter.canConvertTo() == false
    }

    def 'If converting to a GString is attempted, an IllegalStateException is thrown'() {
        when:
        messageConverter.convertTo(null)

        then:
        thrown IllegalStateException
    }

    def 'Validate conversion to a byte array'() {
        setup:
        String subject = "dog"
        GString string = "The ${subject} ran down the street"

        when:
        byte[] converted = messageConverter.convertFrom(string)

        then:
        converted == [84, 104, 101, 32, 100, 111, 103, 32, 114, 97, 110, 32, 100, 111, 119, 110, 32, 116, 104, 101, 32, 115, 116, 114, 101, 101, 116] as byte[]
    }

    def 'Ensure the converter has no associated content type'() {
        messageConverter.getContentType() == null
    }
}
