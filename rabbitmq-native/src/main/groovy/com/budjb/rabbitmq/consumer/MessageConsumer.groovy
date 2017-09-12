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

interface MessageConsumer {
    /**
     * Returns the configuration of the consumer.
     *
     * @return The configuration of the consumer.
     */
    ConsumerConfiguration getConfiguration()

    /**
     * Returns the consumer's ID.
     *
     * @return
     */
    String getId()

    /**
     * Returns the consumer's name.
     */
    String getName()

    /**
     * Processes incoming messages.
     *
     * @param messageContext Contains the various bits of data associated with the incoming message.
     * @return If not null and the message contained a reply-to property, the returned object will be published to the reply-to queue.
     */
    Object process(MessageContext messageContext)

    /**
     * Called when a message is received but before it is handed to the consumer for processing.
     *
     * @param messageContext Contains the various bits of data associated with the incoming message.
     */
    void onReceive(MessageContext messageContext)

    /**
     * Called when message processing has completed successfully.
     *
     * @param messageContext Contains the various bits of data associated with the incoming message.
     */
    void onSuccess(MessageContext messageContext)

    /**
     * Called when message processing has failed with some uncaught exception.
     *
     * @param messageContext Contains the various bits of data associated with the incoming message.
     * @param throwable The exception that occurred.
     */
    void onFailure(MessageContext messageContext, Throwable throwable)

    /**
     * Called when message processing has completed, whether with success or failure.
     *
     * @param messageContext Contains the various bits of data associated with the incoming message.
     */
    void onComplete(MessageContext messageContext)

    /**
     * Initializes the consumer.
     *
     * @throws RuntimeException
     */
    void init() throws RuntimeException
}
