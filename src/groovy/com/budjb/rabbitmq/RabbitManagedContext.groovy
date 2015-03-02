package com.budjb.rabbitmq

interface RabbitManagedContext {
    /**
     * Starts the context.
     *
     * @throws IllegalStateException
     */
    void start() throws IllegalStateException

    /**
     * Stops the context.
     */
    void stop()

    /**
     * Returns the context's ID.
     *
     * @return
     */
    String getId()
}
