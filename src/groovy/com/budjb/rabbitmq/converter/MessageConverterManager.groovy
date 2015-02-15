/*
 * Copyright 2015 Bud Byrd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

class MessageConverterManager implements ApplicationContextAware {
    /**
     * Logger.
     */
    Logger log = Logger.getLogger(MessageConverterManager)

    /**
     * Registered message converters.
     */
    protected List<MessageConverter> messageConverters = []

    /**
     * Grails application bean.
     */
    protected GrailsApplication grailsApplication

    /**
     * Application context.
     */
    protected ApplicationContext applicationContext

    /**
     * Returns the list of registered message converters.
     *
     * @return
     */
    public List<MessageConverter> getMessageConverters() {
        return messageConverters
    }

    /**
     * Registers a message converter.
     *
     * @param messageConverter
     */
    public void registerMessageConverter(MessageConverter messageConverter) {
        messageConverters << messageConverter
    }

    /**
     * Converts a byte array to some other type.
     *
     * @param source Byte array to convert.
     * @return
     * @throws MessageConvertException if there is no converter for the source.
     */
    public Object convertFromBytes(byte[] source) throws MessageConvertException {
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
    public Object convertFromBytes(byte[] source, String contentType) throws MessageConvertException {
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
    public byte[] convertToBytes(Object source) throws MessageConvertException {
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
    public void reset() {
        messageConverters = []
    }

    /**
     * Sets the Grails application bean.
     */
    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
    }

    /**
     * Sets the application context.
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext
    }

    /**
     * Loads message converters.
     */
    public void load() {
        // Register application-provided converters
        grailsApplication.getArtefacts('MessageConverter').each {
            Object consumer = applicationContext.getBean(it.propertyName)
            registerMessageConverter(consumer)
        }

        // Register built-in message converters
        // Note: the order matters, we want string to be the last one
        registerMessageConverter(new IntegerMessageConverter())
        registerMessageConverter(new MapMessageConverter())
        registerMessageConverter(new ListMessageConverter())
        registerMessageConverter(new GStringMessageConverter())
        registerMessageConverter(new StringMessageConverter())
    }
}
