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
package com.budjb.rabbitmq.converter

import com.budjb.rabbitmq.exception.MessageConvertException
import org.apache.log4j.Logger
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClass
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

class MessageConverterManagerImpl implements MessageConverterManager, ApplicationContextAware {
    /**
     * Logger.
     */
    Logger log = Logger.getLogger(MessageConverterManager)

    /**
     * Registered message converters.
     */
    List<MessageConverter<?>> messageConverters = []

    /**
     * Grails application bean.
     */
    GrailsApplication grailsApplication

    /**
     * Application context.
     */
    ApplicationContext applicationContext

    /**
     * Registers a new message converter.
     *
     * @param messageConverter
     */
    void register(MessageConverter<?> messageConverter) {
        messageConverters << messageConverter
    }

    /**
     * Registers a new message converter from its Grails artefact.
     *
     * @param artefact
     */
    void register(GrailsClass artefact) {
        register(applicationContext.getBean(artefact.propertyName))
    }

    /**
     * Converts a byte array to some other type.
     *
     * @param source Byte array to convert.
     * @return
     * @throws MessageConvertException if there is no converter for the source.
     */
    Object convertFromBytes(byte[] source) throws MessageConvertException {
        for (MessageConverter converter in messageConverters) {
            if (!converter.canConvertTo()) {
                continue
            }

            try {
                Object converted = converter.convertTo(source)

                if (converted != null) {
                    return converted
                }
            }
            catch (Exception e) {
                log.error("unhandled exception caught from message converter ${converter.class.simpleName}", e)
            }
        }

        throw new MessageConvertException('no message converter found to convert from a byte array')
    }

    /**
     * Converts a byte array to some other type based on the given content type.
     *
     * @param source Byte array to convert.
     * @param contentType
     * @return
     * @throws MessageConvertException if there is no converter for the source.
     */
    Object convertFromBytes(byte[] source, String contentType) throws MessageConvertException {
        // Find all converters that can handle the content type
        List<MessageConverter> converters = messageConverters.findAll { it.contentType == contentType }

        // If converters are found and it can convert to its type, allow it to do so
        for (MessageConverter converter in converters) {
            if (!converter.canConvertTo()) {
                continue
            }

            try {
                Object converted = converter.convertTo(source)

                if (converted != null) {
                    return converted
                }
            }
            catch (Exception e) {
                log.error("unhandled exception caught from message converter ${converter.class.simpleName}", e)
            }
        }

        throw new MessageConvertException('no message converter found to convert from a byte array')
    }

    /**
     * Converts a given object to a byte array using the message converters.
     *
     * @param source
     * @return
     * @throws MessageConvertException
     */
    byte[] convertToBytes(Object source) throws MessageConvertException {
        // If the source is null, there's nothing to do
        if (source == null) {
            return null
        }

        // Just return the source if it's already a byte array
        if (source instanceof byte[]) {
            return source
        }

        // Try to find a converter that works
        for (MessageConverter converter in messageConverters) {
            if (!converter.canConvertFrom()) {
                continue
            }

            if (!converter.getType().isInstance(source)) {
                continue
            }

            try {
                byte[] converted = converter.convertFrom(source)

                if (converted != null) {
                    return converted
                }
            }
            catch (Exception e) {
                log.error("unhandled exception caught from message converter ${converter.class.simpleName}", e)
            }
        }

        throw new MessageConvertException('no message converter found to convert class ${source.getClass().name} to a byte array')
    }

    /**
     * Resets the message converter manager.
     */
    void reset() {
        messageConverters.clear()
    }

    /**
     * Loads message converters.
     */
    void load() {
        // Register application-provided converters
        grailsApplication.getArtefacts('MessageConverter').each { register(it) }

        // Register built-in message converters
        // Note: the order matters, we want string to be the last one
        register(new IntegerMessageConverter())
        register(new MapMessageConverter())
        register(new ListMessageConverter())
        register(new GStringMessageConverter())
        register(new StringMessageConverter())
    }
}
