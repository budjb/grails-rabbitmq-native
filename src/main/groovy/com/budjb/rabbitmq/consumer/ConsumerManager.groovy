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

import com.budjb.rabbitmq.RabbitManager
import com.budjb.rabbitmq.connection.ConnectionContext
import grails.core.GrailsClass

interface ConsumerManager extends RabbitManager<ConsumerContext, ConsumerConfiguration> {
    /**
     * Create a consumer context with the given consumer object instance.
     *
     * @param consumer
     * @return
     */
    ConsumerContext createContext(MessageConsumer consumer)

    /**
     * Create a consumer context withe consumer represented by the given Grails artefact.
     *
     * @param artefact
     * @return
     */
    ConsumerContext createContext(GrailsClass artefact)

    /**
     * Starts all consumers associated with the given connection context.
     *
     * @param connectionContext
     */
    void start(ConnectionContext connectionContext)

    /**
     * Stops all consumers associated with the given connection context.
     *
     * @param connectionContext
     */
    void stop(ConnectionContext connectionContext)

    /**
     * Retrieves all consumer contexts associated with the given connection context.
     *
     * @param connectionContext
     * @return
     */
    List<ConsumerContext> getContexts(ConnectionContext connectionContext)
}
