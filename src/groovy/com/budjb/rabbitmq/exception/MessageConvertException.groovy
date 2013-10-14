package com.budjb.rabbitmq.exception

class MessageConvertException extends Exception {
    public MessageConvertException(Class clazz) {
        super("unable to convert class ${clazz} to a byte array")
    }
}
