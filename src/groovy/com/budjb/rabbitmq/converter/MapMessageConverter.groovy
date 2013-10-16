package com.budjb.rabbitmq.converter

import com.budjb.rabbitmq.MessageConverter
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

class MapMessageConverter extends MessageConverter<Map> {
    @Override
    public boolean canConvertFrom() {
        return true
    }

    @Override
    public boolean canConvertTo() {
        return true
    }

    @Override
    public Map convertTo(byte[] input) {
        try {
            return new JsonSlurper().parseText(new String(input))
        }
        catch (Exception e) {
            return null
        }
    }

    @Override
    public byte[] convertFrom(Map input) {
        return new JsonBuilder(input).toString().getBytes()
    }

    @Override
    public String getContentType() {
        return 'application/json'
    }

}
