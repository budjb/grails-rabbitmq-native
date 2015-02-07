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
