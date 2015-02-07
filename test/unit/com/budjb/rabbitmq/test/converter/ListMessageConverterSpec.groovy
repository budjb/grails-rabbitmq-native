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
