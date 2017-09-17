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
package com.budjb.rabbitmq.consumer

import com.budjb.rabbitmq.RunningState
import com.budjb.rabbitmq.connection.ConnectionContext
import com.budjb.rabbitmq.connection.ConnectionManager
import com.budjb.rabbitmq.event.ConsumerContextStartedEvent
import com.budjb.rabbitmq.event.ConsumerContextStartingEvent
import com.budjb.rabbitmq.event.ConsumerContextStoppedEvent
import com.budjb.rabbitmq.event.ConsumerContextStoppingEvent
import com.budjb.rabbitmq.exception.ContextNotFoundException
import com.budjb.rabbitmq.publisher.RabbitMessageProperties
import com.budjb.rabbitmq.publisher.RabbitMessagePublisher
import com.budjb.rabbitmq.report.ConsumerReport
import com.rabbitmq.client.Channel
import grails.persistence.support.PersistenceContextInterceptor
import groovyx.gpars.GParsPool
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher

/**
 * Implementation of a container for a consumer and all of its consuming threads.
 */
class ConsumerContextImpl implements ConsumerContext {
    /**
     * Logger.
     */
    protected Logger log = LoggerFactory.getLogger(ConsumerContextImpl)

    /**
     * Consumer bean.
     */
    MessageConsumer consumer

    /**
     * Connection manager.
     */
    ConnectionManager connectionManager

    /**
     * Persistence interceptor for Hibernate session handling.
     */
    PersistenceContextInterceptor persistenceInterceptor

    /**
     * Rabbit message publisher.
     */
    RabbitMessagePublisher rabbitMessagePublisher

    /**
     * Spring application event publisher.
     */
    ApplicationEventPublisher applicationEventPublisher

    /**
     * List of active rabbit consumers.
     */
    protected List<RabbitMessageHandler> consumers = []

    /**
     * Constructor.
     *
     * @param consumer
     * @param connectionManager
     * @param persistenceInterceptor
     * @param rabbitMessagePublisher
     */
    ConsumerContextImpl(
        MessageConsumer consumer,
        ConnectionManager connectionManager,
        PersistenceContextInterceptor persistenceInterceptor,
        RabbitMessagePublisher rabbitMessagePublisher,
        ApplicationEventPublisher applicationEventPublisher) {

        this.consumer = consumer
        this.connectionManager = connectionManager
        this.persistenceInterceptor = persistenceInterceptor
        this.rabbitMessagePublisher = rabbitMessagePublisher
        this.applicationEventPublisher = applicationEventPublisher
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String getConnectionName() {
        return consumer.getConfiguration().getConnection()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String getId() {
        return consumer.getId()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String getName() {
        return consumer.getName()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void start() throws IllegalStateException {
        if (getRunningState() != RunningState.STOPPED) {
            throw new IllegalStateException("attempted to start consumer '${getId()}' but it is already started")
        }

        if (!consumer.getConfiguration()?.isValid()) {
            log.warn("not starting consumer '${getId()}' because it is not valid")
            return
        }

        ConsumerConfiguration configuration = consumer.getConfiguration()

        String connectionName = configuration.getConnection()

        ConnectionContext connectionContext
        try {
            connectionContext = connectionManager.getContext(connectionName)
        }
        catch (ContextNotFoundException ignore) {
            log.warn("not starting consumer '${getId()}' because a suitable connection could not be found")
            return
        }

        if (connectionContext.getRunningState() != RunningState.RUNNING) {
            throw new IllegalStateException("attempted to start consumer '${getId()}' but its connection is not started")
        }

        applicationEventPublisher.publishEvent(new ConsumerContextStartingEvent(this))

        if (configuration.queue) {
            log.debug("starting consumer '${getId()}' on connection '${connectionContext.id}' with ${configuration.consumers} consumer(s)")

            configuration.consumers.times {
                Channel channel = connectionContext.createChannel()

                String queue = configuration.getQueue()

                channel.basicQos(configuration.getPrefetchCount())

                RabbitMessageHandler consumer = new RabbitMessageHandler(channel, queue, this, connectionContext)

                channel.basicConsume(
                    queue,
                    configuration.getAutoAck() == AutoAck.ALWAYS,
                    consumer
                )

                consumers << consumer
            }
        }
        else {
            log.debug("starting consumer '${getId()}' on connection '${connectionContext.id}'")

            Channel channel = connectionContext.createChannel()

            String queue = channel.queueDeclare().getQueue()
            if (!configuration.getBinding() || configuration.getBinding() instanceof String) {
                channel.queueBind(queue, configuration.getExchange(), (String) configuration.getBinding() ?: '')
            }
            else if (configuration.getBinding() instanceof Map) {
                channel.queueBind(queue, configuration.getExchange(), '', (configuration.getBinding() as Map) + ['x-match': configuration.getMatch()])
            }

            channel.basicQos(configuration.getPrefetchCount())

            RabbitMessageHandler consumer = new RabbitMessageHandler(channel, queue, this, connectionContext)

            channel.basicConsume(
                queue,
                configuration.getAutoAck() == AutoAck.ALWAYS,
                consumer
            )

            consumers << consumer
        }

        applicationEventPublisher.publishEvent(new ConsumerContextStartedEvent(this))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void stop() {
        if (getRunningState() == RunningState.STOPPED) {
            return
        }

        applicationEventPublisher.publishEvent(new ConsumerContextStoppingEvent(this))

        consumers.each {
            it.stop()
        }

        consumers.clear()

        log.debug("stopped consumer '${getId()}' on connection '${getConnectionName()}'")
        applicationEventPublisher.publishEvent(new ConsumerContextStoppedEvent(this))
    }

    /**
     * {@inheritDoc}
     */
    RunningState getRunningState() {
        if (consumers.size() == 0) {
            return RunningState.STOPPED
        }

        List<RunningState> states = consumers*.getRunningState()

        if (states.any { it == RunningState.SHUTTING_DOWN }) {
            return RunningState.SHUTTING_DOWN
        }

        if (states.every { it == RunningState.STOPPED }) {
            return RunningState.STOPPED
        }

        return RunningState.RUNNING
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void shutdown() {
        if (getRunningState() != RunningState.RUNNING) {
            return
        }

        log.debug("shutting down consumer '${getId()}' on connection '${getConnectionName()}'")

        GParsPool.withPool {
            consumers.eachParallel { it.shutdown() }
        }

        consumers.clear()

        log.debug("stopped consumer '${getId()}' on connection '${getConnectionName()}'")
    }

    /**
     * {@inheritDoc}
     */
    @Override
    ConsumerReport getStatusReport() {
        ConsumerReport report = new ConsumerReport()

        ConsumerConfiguration configuration = consumer.getConfiguration()

        report.name = consumer.getName()
        report.fullName = getId()

        report.runningState = getRunningState()

        report.queue = configuration.queue ?: consumers.size() > 0 ? consumers[0].queue : null

        report.numConfigured = configuration.consumers
        report.numConsuming = consumers.count { it.getRunningState() == RunningState.RUNNING }
        report.numProcessing = consumers.count { it.isProcessing() }
        report.load = report.numConsuming > 0 ? report.numProcessing / report.numConsuming * 100 : 0

        return report
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void deliverMessage(MessageContext context) {
        Object response
        ConsumerConfiguration configuration = consumer.getConfiguration()

        openSession()
        consumer.onReceive(context)

        try {
            if (configuration.getTransacted()) {
                context.getChannel().txSelect()
            }

            response = consumer.process(context)

            if (configuration.getAutoAck() == AutoAck.POST) {
                context.getChannel().basicAck(context.getEnvelope().deliveryTag, false)
            }

            if (configuration.getTransacted()) {
                context.getChannel().txCommit()
            }

            consumer.onSuccess(context)
        }
        catch (Throwable e) {
            if (configuration.getTransacted()) {
                context.getChannel().txRollback()
            }

            if (configuration.getAutoAck() == AutoAck.POST) {
                context.getChannel().basicReject(context.getEnvelope().deliveryTag, configuration.getRetry())
            }

            if (configuration.getTransacted()) {
                log.error("transaction rolled back due to unhandled exception ${e.getClass().name} caught in RabbitMQ message handler for consumer ${getId()}", e)
            }
            else {
                log.error("unhandled exception ${e.getClass().name} caught in RabbitMQ message handler for consumer ${getId()}", e)
            }

            consumer.onFailure(context, e)

            return
        }
        finally {
            consumer.onComplete(context)

            closeSession()
        }

        if (context.properties.replyTo && response != null) {
            try {
                if (context.properties.replyTo && response) {
                    rabbitMessagePublisher.send(new RabbitMessageProperties(
                        channel: context.channel,
                        routingKey: context.properties.replyTo,
                        correlationId: context.properties.correlationId,
                        body: response
                    ))
                }
            }
            catch (Throwable e) {
                log.error("unexpected exception ${e.getClass()} encountered while responding from an RPC call with handler ${getId()}", e)
            }
        }
    }

    /**
     * Binds a Hibernate session to the current thread if Hibernate is present.
     */
    protected void openSession() {
        if (!persistenceInterceptor) {
            return
        }

        persistenceInterceptor.init()
    }

    /**
     * Closes the bound Hibernate session if Hibernate is present.
     */
    protected void closeSession() {
        if (!persistenceInterceptor) {
            return
        }

        persistenceInterceptor.flush()
        persistenceInterceptor.destroy()
    }
}
