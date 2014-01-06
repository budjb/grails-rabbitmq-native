/*
 * Copyright 2013 Bud Byrd
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

import groovy.util.ConfigObject;
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.apache.log4j.Logger

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

class ConnectionConfiguration {
    /**
     * Logger.
     */
    private Logger log = Logger.getLogger(getClass())

    /**
     * RabbitMQ host
     */
    public String host

    /**
     * RabbitMQ port
     */
    public int port = 5672

    /**
     * RabbitMQ username
     */
    public String username

    /**
     * RabbitMQ password
     */
    public String password

    /**
     * RabbitMQ virtual host
     */
    public String virtualHost = '/'

    /**
     * The maximum number of concurrent consumer threads that are processed.
     *
     * 5 is the RabbitMQ default. 0 means unlimited.
     */
    public int threads = 0

    /**
     * Constructor that parses the configuration for RabbitMQ's connection properties.
     *
     * @param configuration
     */
    public ConnectionConfiguration(ConfigObject configuration) {
        // Make sure we have a configuration
        if (!configuration) {
            throw new Exception('RabbitMQ configuration is missing')
        }

        // Load the configuration
        host = configuration.host ?: null
        if (configuration.port instanceof Integer) {
            port = configuration.port
        }
        username = configuration.username ?: null
        password = configuration.password ?: null
        virtualHost = configuration.virtualHost ?: '/'
        threads = configuration.threads ?: threads

        // Ensure we have all we need to continue
        if (!host || !username || !password) {
            throw new Exception('The host, username, and password configuration options are required for RabbitMQ')
        }
    }

    /**
     * Returns a connection instance based on this context's configuration properties.
     *
     * @return
     */
    public Connection getConnection() {
        // Create the connection factory
        ConnectionFactory factory = new ConnectionFactory()

        // Configure it
        factory.username = username
        factory.password = password
        factory.port = port
        factory.host = host
        factory.virtualHost = virtualHost

        // Create the thread pool service
        ExecutorService executorService
        if (threads > 0) {
            executorService = Executors.newFixedThreadPool(threads)
        }
        else {
            executorService = Executors.newCachedThreadPool()
        }

        return factory.newConnection(executorService)
    }
}
