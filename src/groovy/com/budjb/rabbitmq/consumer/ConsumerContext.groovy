package com.budjb.rabbitmq.consumer

import com.budjb.rabbitmq.RabbitManagedContext

interface ConsumerContext extends RabbitManagedContext {
    /**
     * Return the name of the connection the consumer belongs to.
     *
     * @return
     */
    String getConnectionName()

    /**
     * Returns the consumer's configuration, or null if one is not defined.
     *
     * @return
     */
    ConsumerConfiguration getConfiguration()

    /**
     * Returns whether the consumer's configuration is valid.
     *
     * @return
     */
    boolean isValid()
}
