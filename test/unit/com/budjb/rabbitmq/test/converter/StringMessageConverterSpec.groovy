package com.budjb.rabbitmq.test.converter

import com.budjb.rabbitmq.converter.StringMessageConverter

import spock.lang.Specification

class StringMessageConverterSpec extends Specification {
    StringMessageConverter messageConverter

    def setup() {
        messageConverter = new StringMessageConverter()
    }

    def 'Ensure the converter reports that it can convert from a String'() {
        messageConverter.canConvertFrom() == true
    }

    def 'Ensure the converter reports that it can convert to a String'() {
        messageConverter.canConvertTo() == true
    }

    def 'Validate conversion from a byte array'() {
        setup:
        byte[] source = [84, 104, 101, 32, 100, 111, 103, 32, 114, 97, 110, 32, 100, 111, 119, 110, 32, 116, 104, 101, 32, 115, 116, 114, 101, 101, 116] as byte[]

        when:
        String converted = messageConverter.convertTo(source)

        then:
        converted == 'The dog ran down the street'
    }

    def 'Validate conversion to a byte array'() {
        setup:
        String string = "The dog ran down the street"

        when:
        byte[] converted = messageConverter.convertFrom(string)

        then:
        converted == [84, 104, 101, 32, 100, 111, 103, 32, 114, 97, 110, 32, 100, 111, 119, 110, 32, 116, 104, 101, 32, 115, 116, 114, 101, 101, 116] as byte[]
    }

    def 'Ensure the converter has the correct content type'() {
        messageConverter.getContentType() == 'text/plain'
    }
}
