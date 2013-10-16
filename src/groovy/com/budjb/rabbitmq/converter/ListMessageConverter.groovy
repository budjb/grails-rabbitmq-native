package com.budjb.rabbitmq.converter

import com.budjb.rabbitmq.MessageConverter
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

class ListMessageConverter extends MessageConverter<List> {
    @Override
    public boolean canConvertFrom() {
        return true
    }

    @Override
    public boolean canConvertTo() {
        return true
    }

    @Override
    public List convertTo(byte[] input) {
        try {
            return new JsonSlurper().parseText(new String(input))
        }
        catch (Exception e) {
            return null
        }
    }

    @Override
    public byte[] convertFrom(List input) {
        return new JsonBuilder(input).toString().getBytes()
    }

    @Override
    public String getContentType() {
        return 'application/json'
    }

}
