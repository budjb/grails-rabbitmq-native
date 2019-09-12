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

import com.budjb.rabbitmq.converter.ByteToObjectInput
import com.budjb.rabbitmq.converter.ByteToObjectResult
import com.budjb.rabbitmq.converter.ObjectToByteInput
import com.budjb.rabbitmq.converter.ObjectToByteResult
import com.budjb.rabbitmq.converter.SerializableMessageConverter
import com.budjb.rabbitmq.test.converter.support.MyNotSerializable
import com.budjb.rabbitmq.test.converter.support.MySerializable
import org.springframework.util.MimeType
import spock.lang.*

class SerializableMessageConverterSpec extends Specification {
    SerializableMessageConverter messageConverter

    def setup() {
        messageConverter = new SerializableMessageConverter()
    }

    def 'Validate that the Map and List types are supported'() {
        expect:
        messageConverter.supports(MySerializable)
        !messageConverter.supports(MyNotSerializable)
    }

    def 'Ensure the converter handles the correct content type'() {
        messageConverter.supports(MimeType.valueOf('application/java-serialized-object'))
    }

    def 'Validate conversion from a byte array to a serializable'() {
        setup:
        ByteToObjectInput input = new ByteToObjectInput([-84, -19, 0, 5, 115, 114, 0, 56, 99, 111, 109, 46, 98, 117, 100, 106, 98, 46, 114, 97, 98, 98, 105, 116, 109, 113, 46, 116, 101, 115, 116, 46, 99, 111, 110, 118, 101, 114, 116, 101, 114, 46, 115, 117, 112, 112, 111, 114, 116, 46, 77, 121, 83, 101, 114, 105, 97, 108, 105, 122, 97, 98, 108, 101, -75, -34, -75, -70, -108, 49, 74, 78, 2, 0, 1, 76, 0, 3, 102, 111, 111, 116, 0, 18, 76, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 83, 116, 114, 105, 110, 103, 59, 120, 112, 116, 0, 3, 102, 111, 111] as byte[])

        ByteToObjectResult result
        MySerializable serializable

        when:
        result = messageConverter.convert(input)
        serializable = (MySerializable) result.getResult()

        then:
        serializable.foo == 'foo'
    }

    def 'Validate conversion from a serializable to a byte array'() {
        setup:
        ObjectToByteInput input = new ObjectToByteInput(new MySerializable())

        when:
        ObjectToByteResult result = messageConverter.convert(input)

        then:
        result.getResult() == [-84, -19, 0, 5, 115, 114, 0, 56, 99, 111, 109, 46, 98, 117, 100, 106, 98, 46, 114, 97, 98, 98, 105, 116, 109, 113, 46, 116, 101, 115, 116, 46, 99, 111, 110, 118, 101, 114, 116, 101, 114, 46, 115, 117, 112, 112, 111, 114, 116, 46, 77, 121, 83, 101, 114, 105, 97, 108, 105, 122, 97, 98, 108, 101, -75, -34, -75, -70, -108, 49, 74, 78, 2, 0, 1, 76, 0, 3, 102, 111, 111, 116, 0, 18, 76, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 83, 116, 114, 105, 110, 103, 59, 120, 112, 116, 0, 3, 102, 111, 111] as byte[]
        result.getMimeType().toString() == 'application/java-serialized-object'
    }
}
