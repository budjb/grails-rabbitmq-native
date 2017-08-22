/*
 * Copyright 2017 Bud Byrd
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
package com.budjb.rabbitmq.test.converter

import com.budjb.rabbitmq.converter.*
import org.springframework.util.MimeType
import spock.lang.Specification

class StringMessageConverterSpec extends Specification {
    StringMessageConverter messageConverter

    def setup() {
        messageConverter = new StringMessageConverter()
    }

    def 'Validate that the String type is supported'() {
        expect:
        messageConverter.supports(String)
    }

    def 'Ensure the converter supports the correct content type'() {
        expect:
        messageConverter.supports(MimeType.valueOf('text/plain'))
    }

    def 'Validate conversion from a byte array to a String'() {
        setup:
        ByteToObjectInput input = new ByteToObjectInput([84, 104, 101, 32, 100, 111, 103, 32, 114, 97, 110, 32, 100, 111, 119, 110, 32, 116, 104, 101, 32, 115, 116, 114, 101, 101, 116] as byte[])

        when:
        ByteToObjectResult result = messageConverter.convert(input)

        then:
        result.getResult() == 'The dog ran down the street'
    }

    def 'Validate conversion from a String to a byte array'() {
        setup:
        ObjectToByteInput input = new ObjectToByteInput("The dog ran down the street")

        when:
        ObjectToByteResult result = messageConverter.convert(input)

        then:
        result.getResult() == [84, 104, 101, 32, 100, 111, 103, 32, 114, 97, 110, 32, 100, 111, 119, 110, 32, 116, 104, 101, 32, 115, 116, 114, 101, 101, 116] as byte[]
    }
}
