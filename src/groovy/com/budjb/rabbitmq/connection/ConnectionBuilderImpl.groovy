package com.budjb.rabbitmq.connection

/**
 * Builder class for building connection contexts from a configuration file.
 */
class ConnectionBuilderImpl implements ConnectionBuilder {
    /**
     * Class that handles the connection configuration closure.
     */
    private class ConnectionBuilderDelegate {
        /**
         * Creates a connection context from a configuration or closure method.
         *
         * @param parameters
         * @return
         */
        void connection(Map parameters) {
            // Build the context
            ConnectionContext context = ConnectionBuilderImpl.this.connectionManager.createContext(parameters)

            // Store the context
            ConnectionBuilderImpl.this.connectionContexts << context
        }
    }

    /**
     * Connection manager.
     */
    ConnectionManager connectionManager

    /**
     * List of connections created by the builder.
     */
    private List<ConnectionContext> connectionContexts

    /**
     * Loads connection contexts from a configuration provided by a closure.
     *
     * @param closure
     * @return
     */
    @Override
    List<ConnectionContext> loadConnectionContexts(Closure closure) {
        connectionContexts = []

        ConnectionBuilderDelegate delegate = new ConnectionBuilderDelegate()
        closure.resolveStrategy = Closure.DELEGATE_ONLY
        closure.delegate = delegate
        closure()

        return connectionContexts
    }

    /**
     * Loads connection contexts from a configuration based on a map.
     *
     * @param configuration
     * @return
     */
    @Override
    List<ConnectionContext> loadConnectionContexts(Map configuration) {
        return [connectionManager.createContext(configuration)]
    }
}
