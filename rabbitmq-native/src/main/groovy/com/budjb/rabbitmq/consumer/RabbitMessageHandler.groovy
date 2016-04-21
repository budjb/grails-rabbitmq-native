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
package com.budjb.rabbitmq.consumer

import com.budjb.rabbitmq.RunningState
import com.budjb.rabbitmq.connection.ConnectionContext
import com.rabbitmq.client.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RabbitMessageHandler extends DefaultConsumer {
    /**
     * Logger.
     */
    Logger log = LoggerFactory.getLogger(RabbitMessageHandler)

    /**
     * Consumer context containing the context for this consumer.
     */
    private ConsumerContext consumerContext

    /**
     * Connection context associated with the consumer.
     */
    private ConnectionContext connectionContext

    /**
     * Queue the consumer is connected to. This should only be set when
     * the consumer is subscribing directly to an exchange.
     */
    private String queue

    /**
     * Lock to synchronize access to the processing flag.
     */
    private final Object processLock = new Object()

    /**
     * Current state of the consumer.
     */
    private RunningState runningState = RunningState.STOPPED

    /**
     * Whether the consuming is currently processing a message.
     */
    private boolean processing = false

    /**
     * Constructs an instance of a consumer.
     *
     * @param channel
     * @param context
     */
    RabbitMessageHandler(Channel channel, String queue, ConsumerContextImpl context, ConnectionContext connectionContext) {
        super(channel)

        this.consumerContext = context
        this.connectionContext = connectionContext
        this.queue = queue
    }

    /**
     * Passes delivery of a message back to the context for processing.
     *
     * @param consumerTag
     * @param envelope
     * @param properties
     * @param body
     */
    @Override
    void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
        log.trace("starting handleDelivery for consumer ${consumerContext.getId()}")

        synchronized (processLock) {
            // Mark that the consumer is processing
            processing = true

            // Wrap up the parameters into a context
            MessageContext context = new MessageContext(
                channel: channel,
                consumerTag: consumerTag,
                envelope: envelope,
                properties: properties,
                body: body,
                connectionContext: connectionContext
            )

            // Hand off the message to the context.
            consumerContext.deliverMessage(context)

            // Mark that the consumer is no longer processing
            processing = false
        }

        log.trace("completed handleDelivery for consumer ${consumerContext.getId()}")
    }

    /**
     * Called when the consumer has started consuming.
     *
     * @param consumerTag
     */
    void handleConsumeOk(String consumerTag) {
        super.handleConsumeOk(consumerTag)

        log.trace("received ConsumeOk for consumer ${consumerContext.getId()}")

        runningState = RunningState.RUNNING
    }

    /**
     * Handle unexpected consumer cancels.
     *
     * @param consumerTag
     */
    void handleCancel(String consumerTag) {
        super.handleCancel(consumerTag)

        log.trace("received Cancel for consumer ${consumerContext.getId()}")

        runningState = RunningState.STOPPED
    }

    /**
     * Handle consumer cancel confirmation.
     *
     * @param consumerTag
     */
    void handleCancelOk(String consumerTag) {
        super.handleCancelOk(consumerTag)

        log.trace("received CancelOk for consumer ${consumerContext.getId()}")

        // Only change this if the consumer is running; this will not be true
        // when the consumer is gracefully shutting down.
        if (runningState == RunningState.RUNNING) {
            runningState = RunningState.STOPPED
        }
    }

    /**
     * Handle unexpected channel or connection disconnection.
     *
     * @param consumerTag
     * @param sig
     */
    void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
        super.handleShutdownSignal(consumerTag, sig)

        log.trace("received ShutdownSignal for consumer ${consumerContext.getId()}")

        runningState = RunningState.STOPPED
    }

    /**
     * Handle recovery.
     *
     * @param consumerTag
     */
    void handleRecoverOk(String consumerTag) {
        super.handleRecoverOk(consumerTag)

        log.trace("received RecoverOk for consumer ${consumerContext.getId()}")

        runningState = RunningState.RUNNING
    }

    /**
     * Gracefully stops the consumer, allowing any in-flight processing to complete.
     */
    void shutdown() {
        RunningState previousRunningState = runningState

        runningState = RunningState.SHUTTING_DOWN

        if (previousRunningState == RunningState.RUNNING) {
            channel.basicCancel(getConsumerTag())
        }

        synchronized (processLock) {
            if (channel.isOpen()) {
                channel.close()
            }

            runningState = RunningState.STOPPED
        }
    }

    /**
     * Stops the consumer immediately. Messages being processed will likely fail.
     */
    void stop() {
        if (runningState == RunningState.RUNNING) {
            channel.basicCancel(getConsumerTag())
        }

        if (channel.isOpen()) {
            channel.close()
        }

        runningState = RunningState.STOPPED
    }

    /**
     * Returns the processing state of the consumer.
     *
     * @return
     */
    boolean isProcessing() {
        return processing
    }

    /**
     * Returns the running state of the consumer.
     *
     * @return
     */
    RunningState getRunningState() {
        return runningState
    }

    /**
     * Return the queue the consumer is consuming from.
     *
     * @return
     */
    String getQueue() {
        return queue
    }
}
