/*
 * Copyright 2013-2014 Bud Byrd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.budjb.rabbitmq

import groovy.util.ConfigObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.apache.log4j.Logger

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

class ConnectionBuilder {
    /**
     * Logger.
     */
    protected static Logger log = Logger.getLogger(ConnectionBuilder)

    /**
     * List of loaded connection contexts.
     */
    List<ConnectionContext> connectionContexts = []

    /**
     * Builds connection contexts from the application's configuration.
     */
    public static List<ConnectionContext> loadConnections(def configuration) {
        // Create the connection builder
        ConnectionBuilder connectionBuilder = new ConnectionBuilder()

        // Check if we have a configobject
        if (configuration instanceof ConfigObject) {
            // Load the connection
            connectionBuilder.connection(configuration)

            // Force it to be default
            connectionBuilder.connectionContexts[0].isDefault = true
        }
        else if (configuration instanceof Closure) {
            // Cast the closure
            configuration = (Closure)configuration

            // Set up and run the closure
            configuration = configuration.clone()
            configuration.delegate = connectionBuilder
            configuration.resolveStrategy = Closure.DELEGATE_FIRST
            configuration()

            // If only one connection was configured, force it as default
            if (connectionBuilder.connectionContexts.size() == 1) {
                connectionBuilder.connectionContexts[0].isDefault = true
            }
        }
        else {
            throw new Exception('RabbitMQ connection configuration is not a Grails config or closure')
        }

        return connectionBuilder.connectionContexts
    }

    /**
     * Creates a connection context from a configuration or closure method.
     *
     * @param parameters
     * @return
     */
    public void connection(Map parameters) {
        // Build the context
        ConnectionContext context = new ConnectionContext(parameters)

        // Store the context
        connectionContexts << context
    }
}
