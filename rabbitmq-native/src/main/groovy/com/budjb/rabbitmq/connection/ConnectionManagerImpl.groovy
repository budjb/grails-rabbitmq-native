/*
 * Copyright 2013-2017 Bud Byrd
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

import com.budjb.rabbitmq.RunningState
import com.budjb.rabbitmq.event.ConnectionManagerStartedEvent
import com.budjb.rabbitmq.event.ConnectionManagerStartingEvent
import com.budjb.rabbitmq.event.ConnectionManagerStoppedEvent
import com.budjb.rabbitmq.event.ConnectionManagerStoppingEvent
import com.budjb.rabbitmq.exception.ContextNotFoundException
import com.budjb.rabbitmq.exception.InvalidConfigurationException
import com.budjb.rabbitmq.exception.MissingConfigurationException
import com.budjb.rabbitmq.utils.ConfigPropertyResolver
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import grails.core.GrailsApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher

class ConnectionManagerImpl implements ConnectionManager, ConfigPropertyResolver {
    /**
     * Logger.
     */
    Logger log = LoggerFactory.getLogger(ConnectionManagerImpl)

    /**
     * Grails application bean.
     */
    @Autowired
    GrailsApplication grailsApplication

    /**
     * Spring application event publisher.
     */
    @Autowired
    ApplicationEventPublisher applicationEventPublisher

    /**
     * Connection builder bean.
     */
    @Autowired
    ConnectionBuilder connectionBuilder

    /**
     * Registered connection contexts.
     */
    private List<ConnectionContext> connections = []

    /**
     * Returns the default connection context.
     *
     * @return
     * @throws ContextNotFoundException
     */
    @Override
    ConnectionContext getContext() throws ContextNotFoundException {
        ConnectionContext context = connections.find { it.isDefault }

        if (!context) {
            throw new ContextNotFoundException("no default connection context is configured")
        }

        return context
    }

    /**
     * Returns the connection context with the requested name.
     *
     * @param name
     * @return
     * @throws ContextNotFoundException
     */
    @Override
    ConnectionContext getContext(String name) throws ContextNotFoundException {
        if (name == null) {
            return getContext()
        }

        ConnectionContext context = connections.find { it.id == name }

        if (!context) {
            throw new ContextNotFoundException("no connection context with name '${name}' is configured")
        }

        return context
    }

    /**
     * Creates a new channel with the default connection.
     *
     * @return
     * @throws ContextNotFoundException
     */
    @Override
    Channel createChannel() throws ContextNotFoundException, IllegalStateException {
        return getContext().createChannel()
    }

    /**
     * Creates a new channel with the connection identified by the given connection name.
     *
     * @param connectionName
     * @return
     * @throws ContextNotFoundException
     */
    @Override
    Channel createChannel(String connectionName) throws ContextNotFoundException, IllegalStateException {
        return getContext(connectionName).createChannel()
    }

    /**
     * Returns the RabbitMQ connection associated with the default connection context.
     *
     * @return
     * @throws ContextNotFoundException
     */
    @Override
    Connection getConnection() throws ContextNotFoundException, IllegalStateException {
        return getContext().connection
    }

    /**
     * Returns the RabbitMQ connection associated with the connection context identified
     * by the given connection name.
     *
     * @param connectionName
     * @return
     * @throws ContextNotFoundException
     */
    @Override
    Connection getConnection(String connectionName) throws ContextNotFoundException, IllegalStateException {
        return getContext(connectionName).connection
    }

    /**
     * Loads any configured connections from the grails application configuration.
     */
    @Override
    void load() {
        if (grailsApplication.config.rabbitmq?.connections) {
            // Grail 3 format suitable for YAML configuration. Groovy ConfigSlurper-style
            // configurations will also follow this format going forward.
            def configurations = grailsApplication.config.rabbitmq.connections

            if (!(configurations instanceof Collection)) {
                throw new InvalidConfigurationException("RabbitMQ configuration is invalid; expected a List but got ${configurations.getClass().getSimpleName()} instead")
            }

            configurations.each { item ->
                if (!(item instanceof Map)) {
                    throw new InvalidConfigurationException("RabbitMQ configuration is invalid; expected a Map but got ${configurations.getClass().getSimpleName()} instead")
                }

                register(createContext(fixPropertyResolution(item)))
            }
        }
        else if (grailsApplication.config.rabbitmq?.connection) {
            // Legacy configuration format that supports closures. This functionality
            // is deprecated and will be removed at some point in the future.
            log.warn("Configuration via rabbitmq.connection is deprecated and will be removed in the future.")

            def configuration = grailsApplication.config.rabbitmq?.connection

            if (!(configuration instanceof Map || configuration instanceof Closure)) {
                throw new InvalidConfigurationException('RabbitMQ connection configuration is not a Map or a Closure')
            }

            if (configuration instanceof Map) {
                register(createContext(configuration))
            }
            else {
                connectionBuilder.loadConnectionContexts(configuration as Closure).each { register(it) }
            }
        }
        else {
            throw new MissingConfigurationException("unable to start application because the RabbitMQ connection configuration was not found")
        }
    }

    /**
     * Starts all connection contexts.
     */
    @Override
    void start() {
        if (connections.size() == 0) {
            log.warn("not starting connections because no RabbitMQ connections were configured")
            return
        }

        if (connections.size() == 1) {
            connections[0].isDefault = true
        }

        applicationEventPublisher.publishEvent(new ConnectionManagerStartingEvent(this))

        connections.each {
            try {
                start(it)
            }
            catch (IllegalStateException e) {
                log.trace("error starting a connection; this is probably not a problem", e)
            }
        }

        applicationEventPublisher.publishEvent(new ConnectionManagerStartedEvent(this))
    }

    /**
     * Start a specific connection context.
     *
     * @param context
     */
    @Override
    void start(ConnectionContext context) {
        context.start()
    }

    /**
     * Starts a specific connection context identified by the given connection name.
     *
     * @param name
     * @throws ContextNotFoundException
     */
    @Override
    void start(String name) throws ContextNotFoundException {
        start(getContext(name))
    }

    /**
     * Stops all connection contexts.
     */
    @Override
    void stop() {
        applicationEventPublisher.publishEvent(new ConnectionManagerStoppingEvent(this))

        connections.each { stop(it) }

        applicationEventPublisher.publishEvent(new ConnectionManagerStoppedEvent(this))
    }

    /**
     * Stops a specific connection context.
     *
     * @param context
     */
    @Override
    void stop(ConnectionContext context) {
        context.stop()
    }

    /**
     * Stops a specific connection context identified by the given connection name.
     *
     * @param name
     * @throws ContextNotFoundException
     */
    @Override
    void stop(String name) throws ContextNotFoundException {
        stop(getContext(name))
    }

    /**
     * Removes all connection contexts.
     */
    @Override
    void reset() {
        stop()
        connections.clear()
    }

    /**
     * Registers a new connection context.  If a context already exists with the same name,
     * the old context will be stopped and removed first.
     *
     * @param context
     */
    @Override
    void register(ConnectionContext context) {
        if (context.isDefault) {
            try {
                ConnectionContext defaultContext = getContext()

                if (defaultContext.id != context.id) {
                    throw new InvalidConfigurationException("can not set connection '${context.id}' as default because a default already exists")
                }
            }
            catch (ContextNotFoundException e) {
                log.trace("no default connection context was found; this is ok", e)
            }
        }

        try {
            unregister(getContext(context.id))
        }
        catch (ContextNotFoundException e) {
            log.trace("no connection context with id ${context.id} found; this is ok", e)
        }

        connections << context
    }

    /**
     * Un-registers a connection context.
     *
     * @param context
     */
    @Override
    void unregister(ConnectionContext context) {
        stop(context)
        connections -= context
    }

    /**
     * Creates a new managed context.
     *
     * @param configuration
     * @return
     */
    @Override
    ConnectionContext createContext(ConnectionConfiguration configuration) {
        return new ConnectionContextImpl(configuration, applicationEventPublisher)
    }

    /**
     * Creates a new connection context based on a map of configuration values.
     *
     * @param configuration
     * @return
     */
    @Override
    ConnectionContext createContext(Map configuration) {
        return createContext(new ConnectionConfigurationImpl(configuration))
    }

    /**
     * Returns a list of all registered contexts.
     *
     * @return
     */
    @Override
    List<ConnectionContext> getContexts() {
        return connections
    }

    /**
     * Returns the state of the contexts the manager.
     *
     * @return
     */
    @Override
    RunningState getRunningState() {
        return connections.every {
            it.getRunningState() == RunningState.RUNNING
        } ? RunningState.RUNNING : RunningState.STOPPED
    }
}
