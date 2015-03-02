/*
 * Copyright 2015 Bud Byrd
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

import com.budjb.rabbitmq.RabbitManagedContextConfiguration

interface ConnectionConfiguration extends RabbitManagedContextConfiguration {
    /**
     * Returns the RabbitMQ host.
     */
    String getHost()

    /**
     * Sets the RabbitMQ host.
     */
    void setHost(String host)

    /**
     * Returns the name of the connection.
     */
    String getName()

    /**
     * Sets the name of the connection.
     */
    void setName(String name)

    /**
     * Returns whether the connection is the default connection.
     */
    boolean getIsDefault()

    /**
     * Sets whether the connection is the default connection.
     */
    void setIsDefault(boolean isDefault)

    /**
     * Returns the RabbitMQ broker port.
     */
    int getPort()

    /**
     * Sets the RabbitMQ broker port.
     */
    void setPort(int port)

    /**
     * Returns the RabbitMQ username.
     */
    String getUsername()

    /**
     * Sets the RabbitMQ username.
     */
    void setUsername(String username)

    /**
     * Returns the RabbitMQ password.
     */
    String getPassword()

    /**
     * Sets the RabbitMQ password.
     */
    void setPassword(String password)

    /**
     * Returns the RabbitMQ virtual host.
     */
    String getVirtualHost()

    /**
     * Sets the RabbitMQ virtual host.
     */
    void setVirtualHost(String virtualHost)

    /**
     * Returns whether the connection will automatically reconnect.
     */
    boolean getAutomaticReconnect()

    /**
     * Sets whether the connection will automatically reconnect.
     */
    void setAutomaticReconnect(boolean automaticReconnect)

    /**
     * Returns the maximum number of concurrent consumer threads that are processed.
     */
    int getThreads()

    /**
     * Sets the maximum number of concurrent consumer threads that are processed.
     */
    void setThreads(int threads)

    /**
     * Returns the requested heartbeat delay, in seconds, that the server sends in the connection.tune frame.
     *
     * If set to 0, heartbeats are disabled.
     */
    int getRequestedHeartbeat()

    /**
     * Sets the requested heartbeat delay, in seconds, that the server sends in the connection.tune frame.
     *
     * If set to 0, heartbeats are disabled.
     */
    void setRequestedHeartbeat(int requestedHeartbeat)

    /**
     * Returns whether to use SSL.
     */
    boolean getSsl()

    /**
     * Sets whether to use SSL.
     */
    void setSsl(boolean ssl)
}
