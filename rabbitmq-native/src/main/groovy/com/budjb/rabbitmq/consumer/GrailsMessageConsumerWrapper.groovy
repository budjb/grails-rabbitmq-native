/*
 * Copyright 2017 Bud Byrd
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

import com.budjb.rabbitmq.converter.MessageConverterManager
import grails.core.GrailsApplication

/**
 * A wrapper around consumer classes that look like consumers but do not actually
 * extend {@link GrailsMessageConsumer}.
 */
class GrailsMessageConsumerWrapper extends GrailsMessageConsumer {
    /**
     * The wrapped consumer object.
     */
    Object wrapped

    /**
     * Constructor.
     *
     * @param wrapped The wrapped consumer object.
     */
    GrailsMessageConsumerWrapper(Object wrapped, GrailsApplication grailsApplication, MessageConverterManager messageConverterManager) {
        this.wrapped = wrapped
        this.grailsApplication = grailsApplication
        this.messageConverterManager = messageConverterManager

        init()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Object getActualConsumer() {
        return wrapped
    }
}
