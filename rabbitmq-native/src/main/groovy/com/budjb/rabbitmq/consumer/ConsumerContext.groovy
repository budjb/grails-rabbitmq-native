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

import com.budjb.rabbitmq.RabbitManagedContext
import com.budjb.rabbitmq.report.ConsumerReport

/**
 * A container for a consumer and all of its consuming threads.
 */
interface ConsumerContext extends RabbitManagedContext {
    /**
     * Return the name of the connection the consumer belongs to.
     *
     * @return
     */
    String getConnectionName()

    /**
     * Returns the consumer's name.
     *
     * @return
     */
    String getName()

    /**
     * Performs a graceful shutdown.
     */
    void shutdown()

    /**
     * Generate a status report about the context and its consumers.
     *
     * @return
     */
    ConsumerReport getStatusReport()

    /**
     * Processes and delivers an incoming message to the consumer.
     *
     * @param context
     */
    void deliverMessage(MessageContext context)
}
