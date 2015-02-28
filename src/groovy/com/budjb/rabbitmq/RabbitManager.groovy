package com.budjb.rabbitmq

import com.budjb.rabbitmq.exception.ContextNotFoundException

/**
 * Manager interface that all rabbit managers should implement.
 *
 * @param < C > Managed context type the manager manages.
 */
interface RabbitManager<C extends RabbitManagedContext, I extends RabbitManagedContextConfiguration> {
    /**
     * Load the manager from the Grails application.
     */
    void load()

    /**
     * Starts all managed contexts.
     */
    void start()

    /**
     * Starts a specified context.
     *
     * @param context
     */
    void start(C context)

    /**
     * Starts a specified context based on its ID.
     *
     * @param id
     */
    void start(String id) throws ContextNotFoundException

    /**
     * Stops all managed contexts.
     */
    void stop()

    /**
     * Stops a specified context.
     *
     * @param context
     */
    void stop(C context)

    /**
     * Starts a specified context based on its ID.
     *
     * @param id
     */
    void stop(String id) throws ContextNotFoundException

    /**
     * Removes all managed contexts.
     */
    void reset()

    /**
     * Registers a new managed context.
     *
     * @param context
     */
    void register(C context)

    /**
     * Un-registers a managed context.
     *
     * @param context
     */
    void unregister(C context)

    /**
     * Returns the context identified by the given ID.
     *
     * @param id
     * @return
     */
    C getContext(String id) throws ContextNotFoundException
}
