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
package com.budjb.rabbitmq.exception

import com.budjb.rabbitmq.consumer.MessageConsumer

/**
 * Exception thrown when a {@link com.budjb.rabbitmq.consumer.GrailsMessageConsumer} implementation
 * does not have any message handlers defined.
 */
class NoMessageHandlersDefinedException extends RuntimeException {
    /**
     * The offending message handler class.
     */
    Class<MessageConsumer> messageConsumerClass

    /**
     * Constructor.
     *
     * @param messageConsumerClass
     */
    NoMessageHandlersDefinedException(Class<MessageConsumer> messageConsumerClass) {
        super("message consumer ${messageConsumerClass.getName()} does not have any message handler methods defined")
        this.messageConsumerClass = messageConsumerClass
    }
}
