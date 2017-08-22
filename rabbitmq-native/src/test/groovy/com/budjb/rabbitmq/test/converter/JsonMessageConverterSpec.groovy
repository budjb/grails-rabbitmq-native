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
package com.budjb.rabbitmq.test.converter

import com.budjb.rabbitmq.converter.*
import org.springframework.util.MimeType
import spock.lang.Specification

class JsonMessageConverterSpec extends Specification {
    JsonMessageConverter messageConverter

    def setup() {
        messageConverter = new JsonMessageConverter()
    }

    def 'Validate that the Map and List types are supported'() {
        expect:
        messageConverter.supports(List)
        messageConverter.supports(Map)
    }

    def 'Ensure the converter handles the correct content type'() {
        messageConverter.supports(MimeType.valueOf('application/json'))
    }

    def 'Validate conversion from a byte array to a map'() {
        setup:
        ByteToObjectInput input = new ByteToObjectInput([123, 34, 102, 111, 111, 34, 58, 34, 98, 97, 114, 34, 125] as byte[])

        when:
        ByteToObjectResult result = messageConverter.convert(input)

        then:
        result.getResult() == ["foo": "bar"]
    }

    def 'Validate conversion from a map to a byte array'() {
        setup:
        ObjectToByteInput input = new ObjectToByteInput(["foo": "bar"])

        when:
        ObjectToByteResult result = messageConverter.convert(input)

        then:
        result.getResult() == [123, 34, 102, 111, 111, 34, 58, 34, 98, 97, 114, 34, 125] as byte[]
        result.getMimeType().toString() == 'application/json;charset=UTF-8'
    }

    def 'Validate conversion from a byte array to a list'() {
        setup:
        ByteToObjectInput input = new ByteToObjectInput([91, 34, 102, 111, 111, 34, 44, 34, 98, 97, 114, 34, 93] as byte[])

        when:
        ByteToObjectResult result = messageConverter.convert(input)

        then:
        result.getResult() == ["foo", "bar"]
    }

    def 'Validate conversion from a list to a byte array'() {
        setup:
        ObjectToByteInput input = new ObjectToByteInput(["foo", "bar"])

        when:
        ObjectToByteResult result = messageConverter.convert(input)

        then:
        result.getResult() == [91, 34, 102, 111, 111, 34, 44, 34, 98, 97, 114, 34, 93] as byte[]
        result.getMimeType().toString() == 'application/json;charset=UTF-8'
    }
}
