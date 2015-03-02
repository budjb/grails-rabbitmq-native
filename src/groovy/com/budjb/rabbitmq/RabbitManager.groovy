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
package com.budjb.rabbitmq

import com.budjb.rabbitmq.exception.ContextNotFoundException

/**
 * Manager interface that all rabbit managers should implement.
 *
 * @param < C >  Managed context type the manager manages.
 */
interface RabbitManager<C extends RabbitManagedContext, I extends RabbitManagedContextConfiguration> {
    /**
     * Load the manager from the Grails application.
     */
    void load()

    /**
     * Starts all managed contexts.
     */
    void start()

    /**
     * Starts a specified context.
     *
     * @param context
     */
    void start(C context)

    /**
     * Starts a specified context based on its ID.
     *
     * @param id
     */
    void start(String id) throws ContextNotFoundException

    /**
     * Stops all managed contexts.
     */
    void stop()

    /**
     * Stops a specified context.
     *
     * @param context
     */
    void stop(C context)

    /**
     * Starts a specified context based on its ID.
     *
     * @param id
     */
    void stop(String id) throws ContextNotFoundException

    /**
     * Removes all managed contexts.
     */
    void reset()

    /**
     * Registers a new managed context.
     *
     * @param context
     */
    void register(C context)

    /**
     * Un-registers a managed context.
     *
     * @param context
     */
    void unregister(C context)

    /**
     * Returns the context identified by the given ID.
     *
     * @param id
     * @return
     */
    C getContext(String id) throws ContextNotFoundException
}
