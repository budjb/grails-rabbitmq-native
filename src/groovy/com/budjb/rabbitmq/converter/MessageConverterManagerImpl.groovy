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
    @Override
    void register(MessageConverter<?> messageConverter) {
        messageConverters << messageConverter
    }

    /**
     * Registers a new message converter from its Grails artefact.
     *
     * @param artefact
     */
    @Override
    void register(GrailsClass artefact) {
        register(applicationContext.getBean(artefact.propertyName, MessageConverter))
    }

    /**
     * Attempt to marshall a byte array to some other object type.
     *
     * @param source
     * @return
     */
    @Override
    Object convertFromBytes(byte[] source) {
        return convertFromBytes(source, [Object])
    }

    /**
     * Converts a byte array to some other type based on the given content type.
     *
     * @param source Byte array to convert.
     * @param contentType
     * @return
     * @throws MessageConvertException if there is no converter for the source.
     */
    @Override
    Object convertFromBytes(byte[] source, List<Class<?>> availableClasses, String contentType = null) throws MessageConvertException {
        List<MessageConverter> converters = messageConverters

        if (contentType != null) {
            converters = converters.findAll { it.contentType == contentType }
        }

        for (MessageConverter converter in converters) {
            if (!converter.canConvertTo()) {
                continue
            }

            if (!availableClasses.any { it.isAssignableFrom(converter.getType()) }) {
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
    @Override
    byte[] convertToBytes(Object source) throws MessageConvertException {
        if (source == null) {
            return null
        }

        if (source instanceof byte[]) {
            return source
        }

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
    @Override
    void reset() {
        messageConverters.clear()
    }

    /**
     * Loads message converters.
     */
    @Override
    void load() {
        grailsApplication.getArtefacts('MessageConverter').each { register(it) }

        // Note: the order matters, we want string to be the last one
        register(new IntegerMessageConverter())
        register(new MapMessageConverter())
        register(new ListMessageConverter())
        register(new GStringMessageConverter())
        register(new StringMessageConverter())
    }
}
