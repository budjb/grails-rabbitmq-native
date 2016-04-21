/*
 * Copyright 2016 Bud Byrd
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
package com.budjb.rabbitmq.connection

import com.budjb.rabbitmq.RabbitManager
import com.budjb.rabbitmq.exception.ContextNotFoundException
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection

interface ConnectionManager extends RabbitManager<ConnectionContext, ConnectionConfiguration> {
    /**
     * Return the default connection context.
     *
     * @return
     */
    ConnectionContext getContext() throws ContextNotFoundException

    /**
     * Create a channel with the default connection context.
     *
     * @return
     */
    Channel createChannel() throws ContextNotFoundException, IllegalStateException

    /**
     * Create a channel with the connection context identified by the given name.
     *
     * @param connectionName
     * @return
     */
    Channel createChannel(String connectionName) throws ContextNotFoundException, IllegalStateException

    /**
     * Return the connection associated with the default connection context.
     *
     * @return
     */
    Connection getConnection() throws ContextNotFoundException, IllegalStateException

    /**
     * Return the connection associated with the connection context identified by the given connection name.
     *
     * @param connectionName
     * @return
     */
    Connection getConnection(String connectionName) throws ContextNotFoundException, IllegalStateException

    /**
     * Create a connection context based on a map of configuration values.
     *
     * @param configuration
     * @return
     */
    ConnectionContext createContext(Map configuration)

    /**
     * Create a new connection context with the given configuration.
     *
     * @param configuration
     * @return
     */
    ConnectionContext createContext(ConnectionConfiguration configuration)
}
