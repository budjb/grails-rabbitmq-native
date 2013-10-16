package com.budjb.rabbitmq.converter

import com.budjb.rabbitmq.MessageConverter

class StringMessageConverter extends MessageConverter<String> {
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

    @Override
    public String getContentType() {
        return 'text/plain'
    }

}
