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
import grails.config.Config
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
     * {@inheritDoc}
     */
    @Override
    Config getGrailsConfiguration() {
        return grailsApplication.getConfig()
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    @Override
    Channel createChannel() throws ContextNotFoundException, IllegalStateException {
        return getContext().createChannel()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Channel createChannel(String connectionName) throws ContextNotFoundException, IllegalStateException {
        return getContext(connectionName).createChannel()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Connection getConnection() throws ContextNotFoundException, IllegalStateException {
        return getContext().connection
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Connection getConnection(String connectionName) throws ContextNotFoundException, IllegalStateException {
        return getContext(connectionName).connection
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void load() {
        if (!grailsApplication.getConfig().containsProperty('rabbitmq.connections')) {
            throw new MissingConfigurationException("unable to start application because the RabbitMQ connection configuration was not found")
        }

        List connections = grailsApplication.getConfig().getProperty('rabbitmq.connections', List)

        if (!connections) {
            throw new InvalidConfigurationException("RabbitMQ connection configuration is either invalid or missing")
        }

        connections.each { item ->
            if (!Map.isInstance(item)) {
                throw new InvalidConfigurationException("RabbitMQ connection configuration is expected to be a map but got ${item.getClass().getName()} instead")
            }

            register(createContext(fixPropertyResolution((Map) item)))
        }
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    @Override
    void start(ConnectionContext context) {
        context.start()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void start(String name) throws ContextNotFoundException {
        start(getContext(name))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void stop() {
        applicationEventPublisher.publishEvent(new ConnectionManagerStoppingEvent(this))

        connections.each { stop(it) }

        applicationEventPublisher.publishEvent(new ConnectionManagerStoppedEvent(this))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void stop(ConnectionContext context) {
        context.stop()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void stop(String name) throws ContextNotFoundException {
        stop(getContext(name))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void reset() {
        stop()
        connections.clear()
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    @Override
    void unregister(ConnectionContext context) {
        stop(context)
        connections -= context
    }

    /**
     * {@inheritDoc}
     */
    @Override
    ConnectionContext createContext(ConnectionConfiguration configuration) {
        return new ConnectionContextImpl(configuration, applicationEventPublisher)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    ConnectionContext createContext(Map configuration) {
        return createContext(new ConnectionConfigurationImpl(configuration))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    List<ConnectionContext> getContexts() {
        return connections
    }

    /**
     * {@inheritDoc}
     */
    @Override
    RunningState getRunningState() {
        return connections.every {
            it.getRunningState() == RunningState.RUNNING
        } ? RunningState.RUNNING : RunningState.STOPPED
    }
}
