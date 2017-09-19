package com.budjb.rabbitmq.consumer

trait MessageConsumerEventHandler {
    /**
     * Called when a message is received but before it is handed to the consumer for processing.
     *
     * @param messageContext Contains the various bits of data associated with the incoming message.
     */
    void onReceive(MessageContext messageContext) {

    }

    /**
     * Called when message processing has completed successfully.
     *
     * @param messageContext Contains the various bits of data associated with the incoming message.
     */
    void onSuccess(MessageContext messageContext) {

    }

    /**
     * Called when message processing has failed with some uncaught exception.
     *
     * @param messageContext Contains the various bits of data associated with the incoming message.
     * @param throwable The exception that occurred.
     */
    void onFailure(MessageContext messageContext, Throwable throwable) {

    }

    /**
     * Called when message processing has completed, whether with success or failure.
     *
     * @param messageContext Contains the various bits of data associated with the incoming message.
     */
    void onComplete(MessageContext messageContext) {

    }
}
