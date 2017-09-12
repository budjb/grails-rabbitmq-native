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

/**
 * The base message consumer class to reduce the amount of boilerplate code needed to define a consumer.
 */
abstract class AbstractMessageConsumer implements MessageConsumer {
    /**
     * {@inheritDoc}
     */
    @Override
    String getId() {
        return getActualConsumer().getClass().getName()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String getName() {
        return getActualConsumer().getClass().getSimpleName()
    }

    /**
     * {@inheritDoc}
     */
    Object getActualConsumer() {
        return this
    }

    /**
     * {@inheritDoc}
     */
    void onReceive(MessageContext messageContext) {

    }

    /**
     * {@inheritDoc}
     */
    void onSuccess(MessageContext messageContext) {

    }

    /**
     * {@inheritDoc}
     */
    void onFailure(MessageContext messageContext, Throwable throwable) {

    }

    /**
     * {@inheritDoc}
     */
    void onComplete(MessageContext messageContext) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    void init() throws RuntimeException {

    }
}
