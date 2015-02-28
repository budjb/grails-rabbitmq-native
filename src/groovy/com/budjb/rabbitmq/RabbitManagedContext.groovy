package com.budjb.rabbitmq

interface RabbitManagedContext {
    /**
     * Starts the context.
     */
    void start()

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
