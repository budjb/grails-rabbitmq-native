package com.budjb.rabbitmq.connection

/**
 * Created by bud on 2/27/15.
 */
interface ConnectionBuilder {
    /**
     * Loads connection contexts from a configuration based on a map.
     *
     * @param configuration
     * @return
     */
    List<ConnectionContext> loadConnectionContexts(Map configuration)

    /**
     * Loads connection contexts from a configuration provided by a closure.
     *
     * @param closure
     * @return
     */
    List<ConnectionContext> loadConnectionContexts(Closure closure)
}
