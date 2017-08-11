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
        afterPropertiesSet()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Object getActualConsumer() {
        return wrapped
    }
}
