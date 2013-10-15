package com.budjb.rabbitmq.converter

import com.budjb.rabbitmq.MessageConverter

class StringMessageConverter implements MessageConverter<String> {
    @Override
    public Class getType() {
        return String
    }

    @Override
    public boolean canConvertFrom() {
        return true
    }

    @Override
    public boolean canConvertTo() {
        return true
    }

    @Override
    public String convertTo(byte[] input) {
        return new String(input)
    }

    @Override
    public byte[] convertFrom(String input) {
        return input.getBytes()
    }

}
