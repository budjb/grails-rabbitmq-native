package com.budjb.rabbitmq.consumer

interface UnsupportedMessageHandler {
    /**
     * Called when there are no message converters and consumer handlers available to process an incoming message.
     *
     * @param messageContext Contains the various bits of data associated with the incoming message.
     * @return Optionally respond to an RPC request with the return of the method. If null, no response will be sent.
     */
    Object handleUnsupportedMessage(MessageContext messageContext)
}
