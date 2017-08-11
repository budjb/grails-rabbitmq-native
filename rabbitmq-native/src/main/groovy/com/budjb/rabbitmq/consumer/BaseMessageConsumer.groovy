package com.budjb.rabbitmq.consumer

/**
 * The base message consumer class to reduce the amount of boilerplate code needed to define a consumer.
 */
abstract class BaseMessageConsumer implements MessageConsumer {
    /**
     * {@inheritDoc}
     */
    @Override
    String getId() {
        return getActualConsumer().getClass().getName()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String getName() {
        return getActualConsumer().getClass().getSimpleName()
    }

    /**
     * Returns the actual consumer object. Will usually be the same instance the method
     * is called on, but may be different in the case of wrappers.
     *
     * @return The actual consumer object.
     */
    Object getActualConsumer() {
        return this
    }

    /**
     * Called when a message has been received.
     *
     * @param messageContext
     */
    void onReceive(MessageContext messageContext) {

    }

    /**
     * Called when a message has completed processing successfully.
     *
     * @param messageContext
     */
    void onSuccess(MessageContext messageContext) {

    }

    /**
     * Called when an exception has occurred while processing a message.
     *
     * @param messageContext
     * @param throwable
     */
    void onFailure(MessageContext messageContext, Throwable throwable) {

    }

    /**
     * Called when processing of a message is complete. This will happen in
     * both the success and failure cases in addition to the success or
     * failure callbacks.
     *
     * @param messageContext
     */
    void onComplete(MessageContext messageContext) {

    }
}
