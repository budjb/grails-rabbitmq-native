package com.budjb.rabbitmq.exception

class DuplicateHandlerException extends RuntimeException {
    final Class<?> type

    DuplicateHandlerException(Class<?> type) {
        super("multiple message handlers that support type ${type.getName()} were found in a single message consumer")
        this.type = type
    }
}
