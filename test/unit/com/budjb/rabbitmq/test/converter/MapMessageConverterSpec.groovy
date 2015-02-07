package com.budjb.rabbitmq.test.converter

import com.budjb.rabbitmq.converter.MapMessageConverter

import spock.lang.Specification

class MapMessageConverterSpec extends Specification {
    MapMessageConverter messageConverter

    def setup() {
        messageConverter = new MapMessageConverter()
    }

    def 'Ensure the converter reports that it can convert from a map'() {
        messageConverter.canConvertFrom() == true
    }

    def 'Ensure the converter reports that it can convert to a map'() {
        messageConverter.canConvertTo() == true
    }

    def 'Validate conversion from a byte array'() {
        setup:
        byte[] source = [123, 34, 102, 111, 111, 34, 58, 34, 98, 97, 114, 34, 125] as byte[]

        when:
        Map converted = messageConverter.convertTo(source)

        then:
        converted == ["foo": "bar"]
    }

    def 'Validate conversion to a byte array'() {
        setup:
        Map list = ["foo": "bar"]

        when:
        byte[] converted = messageConverter.convertFrom(list)

        then:
        converted == [123, 34, 102, 111, 111, 34, 58, 34, 98, 97, 114, 34, 125] as byte[]
    }

    def 'Ensure the converter has the correct content type'() {
        messageConverter.getContentType() == 'application/json'
    }
}
