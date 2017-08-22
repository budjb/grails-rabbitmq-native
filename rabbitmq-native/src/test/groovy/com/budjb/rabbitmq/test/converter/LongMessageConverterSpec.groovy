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

class LongMessageConverterSpec extends Specification {
    LongMessageConverter messageConverter

    def setup() {
        messageConverter = new LongMessageConverter()
    }

    def 'Validate that the Long data type is supported'() {
        expect:
        messageConverter.supports(long)
        messageConverter.supports(Long)
    }

    def 'Validate that the proper content type is supported'() {
        expect:
        messageConverter.supports(MimeType.valueOf('text/plain'))
    }

    def 'Validate conversion from a byte array to a Long '() {
        setup:
        ByteToObjectInput input = new ByteToObjectInput([49, 50, 51, 52] as byte[])

        when:
        ByteToObjectResult result = messageConverter.convert(input)

        then:
        result.getResult() == 1234
    }

    def 'Validate conversion fro a Long to a byte array'() {
        setup:
        ObjectToByteInput input = new ObjectToByteInput(1234)

        when:
        ObjectToByteResult result = messageConverter.convert(input)

        then:
        result.getResult() == [49, 50, 51, 52] as byte[]
        result.getMimeType().toString() == 'text/plain;charset=UTF-8'
    }
}
