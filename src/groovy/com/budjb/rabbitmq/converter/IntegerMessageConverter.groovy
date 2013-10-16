package com.budjb.rabbitmq.converter

import com.budjb.rabbitmq.MessageConverter

class IntegerMessageConverter extends MessageConverter<Integer> {
    @Override
    public boolean canConvertFrom() {
        return true
    }

    @Override
    public boolean canConvertTo() {
        return true
    }

    @Override
    public Integer convertTo(byte[] input) {
        String string = new String(input)
        if (!string.isInteger()) {
            return null
        }
        return string.toInteger()
    }

    @Override
    public byte[] convertFrom(Integer input) {
        return input.toString().getBytes()
    }

    @Override
    public String getContentType() {
        return null
    }

}
