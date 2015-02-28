package com.budjb.rabbitmq.exception

class ContextNotFoundException extends Exception {
    ContextNotFoundException() {
        super()
    }

    ContextNotFoundException(String message) {
        super(message)
    }

    ContextNotFoundException(String message, Throwable cause) {
        super(message, cause)
    }
}
