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
package com.budjb.rabbitmq.consumer

abstract class MessageConsumer {
    /**
     * Returns the consumer configuration.
     *
     * @return
     */
    Map getRabbitConfig() {
        return null
    }

    /**
     * A basic message handler that consumes a message without any content marshalling.
     *
     * @param message
     * @param messageContext
     */
    void handleMessage(byte[] message, MessageContext messageContext) { }

    /**
     * Called when a message has been successfully been consumed without any uncaught exceptions.
     *
     * @param messageContext
     */
    void onSuccess(MessageContext messageContext) { }

    /**
     * Called when a message has been consumed with an uncaught exception.
     *
     * @param messageContext
     */
    void onFailure(MessageContext messageContext) { }

    /**
     * Called when a message has completed processing, either with success or failure.
     *
     * @param messageContext
     */
    void onComplete(MessageContext messageContext) { }

    /**
     * Called when a message is received and before it is given to a message handler.
     *
     * @param messageContext
     */
    void onReceive(MessageContext messageContext) { }
}
