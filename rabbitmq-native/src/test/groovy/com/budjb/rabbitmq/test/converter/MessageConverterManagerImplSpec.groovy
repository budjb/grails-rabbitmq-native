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
import com.budjb.rabbitmq.exception.NoConverterFoundException
import grails.core.GrailsApplication
import grails.core.GrailsClass
import grails.util.TypeConvertingMap
import org.apache.groovy.json.internal.LazyMap
import org.springframework.context.ApplicationContext
import org.springframework.util.MimeType
import spock.lang.Specification
import spock.lang.Unroll

class MessageConverterManagerImplSpec extends Specification {
    MessageConverterManagerImpl messageConverterManager
    GrailsApplication grailsApplication
    ApplicationContext applicationContext

    def setup() {
        grailsApplication = Mock(GrailsApplication)
        applicationContext = Mock(ApplicationContext)

        messageConverterManager = new MessageConverterManagerImpl()
        messageConverterManager.grailsApplication = grailsApplication
        messageConverterManager.applicationContext = applicationContext
        messageConverterManager.enableSerializableConverter = true
    }

    def 'Ensure setGrailsApplication(GrailsApplication) sets the property correctly'() {
        setup:
        GrailsApplication grailsApplication = Mock(GrailsApplication)

        when:
        messageConverterManager.setGrailsApplication(grailsApplication)

        then:
        messageConverterManager.grailsApplication == grailsApplication
    }

    def 'Validate registering and retrieving message converters'() {
        setup:
        StringMessageConverter stringMessageConverter = Mock(StringMessageConverter)
        LongMessageConverter integerMessageConverter = Mock(LongMessageConverter)

        when:
        messageConverterManager.register(integerMessageConverter)
        messageConverterManager.register(stringMessageConverter)

        then:
        messageConverterManager.messageConverters.size() == 2

        when:
        List converters = messageConverterManager.getMessageConverters()

        then:
        converters.size() == 2
        converters == [integerMessageConverter, stringMessageConverter]
    }

    def 'Validate that all built-in converters can convert from bytes through the manager'() {
        setup:
        grailsApplication.getArtefacts('MessageConverter') >> []
        messageConverterManager.load()

        expect:
        messageConverterManager.convert(new ByteToObjectInput(a)).getResult() == b

        where:
        a                                                                    || b
        [102, 111, 111, 98, 97, 114] as byte[]                               || "foobar"
        [49, 50, 51, 52] as byte[]                                           || 1234
        [91, 34, 102, 111, 111, 34, 44, 34, 98, 97, 114, 34, 93] as byte[]   || ["foo", "bar"]
        [123, 34, 102, 111, 111, 34, 58, 34, 98, 97, 114, 34, 125] as byte[] || ["foo": "bar"] as TypeConvertingMap
    }

    @Unroll
    def 'Validate #a can be converted to bytes through the manager'() {
        setup:
        grailsApplication.getArtefacts('MessageConverter') >> []
        messageConverterManager.load()

        expect:
        messageConverterManager.convert(new ObjectToByteInput(a)).getResult() == b

        where:
        a                         || b
        "foobar"                  || [-84, -19, 0, 5, 116, 0, 6, 102, 111, 111, 98, 97, 114] as byte[]
        1234                      || [-84, -19, 0, 5, 115, 114, 0, 17, 106, 97, 118, 97, 46, 108, 97, 110, 103, 46, 73, 110, 116, 101, 103, 101, 114, 18, -30, -96, -92, -9, -127, -121, 56, 2, 0, 1, 73, 0, 5, 118, 97, 108, 117, 101, 120, 114, 0, 16, 106, 97, 118, 97, 46, 108, 97, 110, 103, 46, 78, 117, 109, 98, 101, 114, -122, -84, -107, 29, 11, -108, -32, -117, 2, 0, 0, 120, 112, 0, 0, 4, -46] as byte[]
        ["foo", "bar"]            || [-84, -19, 0, 5, 115, 114, 0, 19, 106, 97, 118, 97, 46, 117, 116, 105, 108, 46, 65, 114, 114, 97, 121, 76, 105, 115, 116, 120, -127, -46, 29, -103, -57, 97, -99, 3, 0, 1, 73, 0, 4, 115, 105, 122, 101, 120, 112, 0, 0, 0, 2, 119, 4, 0, 0, 0, 2, 116, 0, 3, 102, 111, 111, 116, 0, 3, 98, 97, 114, 120] as byte[]
        ["foo": "bar"]            || [-84, -19, 0, 5, 115, 114, 0, 23, 106, 97, 118, 97, 46, 117, 116, 105, 108, 46, 76, 105, 110, 107, 101, 100, 72, 97, 115, 104, 77, 97, 112, 52, -64, 78, 92, 16, 108, -64, -5, 2, 0, 1, 90, 0, 11, 97, 99, 99, 101, 115, 115, 79, 114, 100, 101, 114, 120, 114, 0, 17, 106, 97, 118, 97, 46, 117, 116, 105, 108, 46, 72, 97, 115, 104, 77, 97, 112, 5, 7, -38, -63, -61, 22, 96, -47, 3, 0, 2, 70, 0, 10, 108, 111, 97, 100, 70, 97, 99, 116, 111, 114, 73, 0, 9, 116, 104, 114, 101, 115, 104, 111, 108, 100, 120, 112, 63, 64, 0, 0, 0, 0, 0, 1, 119, 8, 0, 0, 0, 2, 0, 0, 0, 1, 116, 0, 3, 102, 111, 111, 116, 0, 3, 98, 97, 114, 120, 0] as byte[]
        "${'foobar'}"             || [-84, -19, 0, 5, 115, 114, 0, 39, 111, 114, 103, 46, 99, 111, 100, 101, 104, 97, 117, 115, 46, 103, 114, 111, 111, 118, 121, 46, 114, 117, 110, 116, 105, 109, 101, 46, 71, 83, 116, 114, 105, 110, 103, 73, 109, 112, 108, 49, -77, 75, 103, -5, 125, -23, 18, 2, 0, 1, 91, 0, 7, 115, 116, 114, 105, 110, 103, 115, 116, 0, 19, 91, 76, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 83, 116, 114, 105, 110, 103, 59, 120, 114, 0, 19, 103, 114, 111, 111, 118, 121, 46, 108, 97, 110, 103, 46, 71, 83, 116, 114, 105, 110, 103, -37, 99, -34, 118, -112, -53, 8, -51, 2, 0, 1, 91, 0, 6, 118, 97, 108, 117, 101, 115, 116, 0, 19, 91, 76, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 79, 98, 106, 101, 99, 116, 59, 120, 112, 117, 114, 0, 19, 91, 76, 106, 97, 118, 97, 46, 108, 97, 110, 103, 46, 79, 98, 106, 101, 99, 116, 59, -112, -50, 88, -97, 16, 115, 41, 108, 2, 0, 0, 120, 112, 0, 0, 0, 1, 116, 0, 6, 102, 111, 111, 98, 97, 114, 117, 114, 0, 19, 91, 76, 106, 97, 118, 97, 46, 108, 97, 110, 103, 46, 83, 116, 114, 105, 110, 103, 59, -83, -46, 86, -25, -23, 29, 123, 71, 2, 0, 0, 120, 112, 0, 0, 0, 2, 116, 0, 0, 113, 0, 126, 0, 10] as byte[]
        new LazyMap([foo: 'bar']) || [123, 34, 102, 111, 111, 34, 58, 34, 98, 97, 114, 34, 125]
    }

    def 'Ensure message converters are freed up with reset() is called'() {
        setup:
        grailsApplication.getArtefacts('MessageConverter') >> []
        messageConverterManager.load()

        when:
        messageConverterManager.reset()

        then:
        messageConverterManager.getMessageConverters().size() == 0
    }

    def 'If Grails reports custom message converters, make sure they\'re loaded'() {
        setup:
        ExpandoMockConverter customConverter = new ExpandoMockConverter()
        GrailsClass grailsClass = Mock(GrailsClass)
        grailsClass.getPropertyName() >> "ExpandoMockConverter"
        applicationContext.getBean("ExpandoMockConverter", _) >> customConverter
        grailsApplication.getArtefacts('MessageConverter') >> [grailsClass]

        when:
        messageConverterManager.load()

        then:
        messageConverterManager.getMessageConverters().size() == 6
        messageConverterManager.getMessageConverters().contains(customConverter)

        when:
        ByteToObjectResult convertedFromBytes = messageConverterManager.convert(new ByteToObjectInput([102, 111, 111, 98, 97, 114, 98, 97, 122] as byte[]))

        then:
        convertedFromBytes.getResult() instanceof Expando

        when:
        ObjectToByteResult convertedFromObject = messageConverterManager.convert(new ObjectToByteInput(new Expando()))

        then:
        convertedFromObject.getResult() == [102, 111, 111, 98, 97, 114, 98, 97, 122] as byte[]
    }

    class ExpandoMockConverter implements ByteToObjectConverter, ObjectToByteConverter {
        @Override
        boolean supports(Class<?> type) {
            return Expando.isAssignableFrom(type)
        }

        @Override
        boolean supports(MimeType mimeType) {
            return true
        }

        @Override
        ByteToObjectResult convert(ByteToObjectInput input) {
            if (input.getBytes() == [102, 111, 111, 98, 97, 114, 98, 97, 122] as byte[]) {
                return new ByteToObjectResult(new Expando())
            }
            return null
        }

        @Override
        ObjectToByteResult convert(ObjectToByteInput input) {
            return new ObjectToByteResult([102, 111, 111, 98, 97, 114, 98, 97, 122] as byte[], null)
        }
    }

    def 'If the serializable converter is disabled, it is not used'() {
        setup:
        SerializableMessageConverter serializableMessageConverter = Mock(SerializableMessageConverter)
        serializableMessageConverter.supports((Class)_) >> true
        serializableMessageConverter.supports((MimeType)_) >> true

        ObjectToByteConverter converter = Mock(ObjectToByteConverter)
        converter.supports((Class)_) >> true
        converter.supports((MimeType)_) >> true

        messageConverterManager.messageConverters = [converter]
        messageConverterManager.serializableMessageConverter = serializableMessageConverter
        messageConverterManager.enableSerializableConverter = false

        ObjectToByteInput input = new ObjectToByteInput('foo')

        when:
        messageConverterManager.convert(input)

        then:
        thrown NoConverterFoundException
        0 * serializableMessageConverter.convert(input)
        1 * converter.convert(input)
    }
}
