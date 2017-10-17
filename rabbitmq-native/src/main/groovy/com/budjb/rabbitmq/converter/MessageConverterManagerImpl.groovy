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
package com.budjb.rabbitmq.converter

import com.budjb.rabbitmq.consumer.MessageConvertMethod
import com.budjb.rabbitmq.exception.NoConverterFoundException
import grails.config.Config
import grails.core.GrailsApplication
import grails.core.GrailsClass
import grails.core.support.GrailsConfigurationAware
import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.util.ClassUtils
import org.springframework.util.MimeType

/**
 * A class that manages message converters and acts as the entry point for conversion.
 */
@CompileStatic
class MessageConverterManagerImpl implements MessageConverterManager, ApplicationContextAware, GrailsConfigurationAware {
    /**
     * Binary data mime type.
     */
    final static MimeType APPLICATION_OCTET_STREAM = MimeType.valueOf('application/octet-stream')

    /**
     * Logger.
     */
    Logger log = LoggerFactory.getLogger(MessageConverterManager)

    /**
     * Serializable message converter. This is kept separate because it needs to be called before
     * any other message converters.
     */
    SerializableMessageConverter serializableMessageConverter = new SerializableMessageConverter()

    /**
     * Registered message converters.
     */
    List<MessageConverter> messageConverters = []

    /**
     * Grails application bean.
     */
    @Autowired
    GrailsApplication grailsApplication

    /**
     * Application context.
     */
    ApplicationContext applicationContext

    /**
     * Whether to use the serializable converter.
     */
    boolean enableSerializableConverter = false

    /**
     * {@inheritDoc}
     */
    @Override
    List<ByteToObjectConverter> getByteToObjectConverters() {
        List<ByteToObjectConverter> converters = []

        for (MessageConverter messageConverter : getMessageConverters()) {
            if (ByteToObjectConverter.isInstance(messageConverter)) {
                converters.add((ByteToObjectConverter) messageConverter)
            }
        }

        return converters
    }

    /**
     * {@inheritDoc}
     */
    @Override
    List<ObjectToByteConverter> getObjectToByteConverters() {
        List<ObjectToByteConverter> converters = []

        for (MessageConverter messageConverter : getMessageConverters()) {
            if (ObjectToByteConverter.isInstance(messageConverter)) {
                converters.add((ObjectToByteConverter) messageConverter)
            }
        }

        return converters

    }

    /**
     * {@inheritDoc}
     */
    @Override
    ObjectToByteResult convert(ObjectToByteInput input) throws NoConverterFoundException {
        Object body = input.getObject()

        if (body == null) {
            return null
        }

        if (body instanceof byte[]) {
            return new ObjectToByteResult((byte[]) body, APPLICATION_OCTET_STREAM)
        }

        if (body instanceof Serializable) {
            if (enableSerializableConverter) {
                ObjectToByteResult converted = attemptConversion(serializableMessageConverter, input)

                if (converted != null) {
                    return converted
                }
            }
        }

        Class<?> type = body.getClass()

        for (ObjectToByteConverter messageConverter : getObjectToByteConverters()) {
            if (messageConverter.supports(type)) {
                ObjectToByteResult converted = attemptConversion(messageConverter, input)

                if (converted != null) {
                    return converted
                }
            }
        }

        throw new NoConverterFoundException('no message converter found to convert to a byte array')
    }

    /**
     * {@inheritDoc}
     */
    @Override
    ByteToObjectResult convert(ByteToObjectInput input) throws NoConverterFoundException {
        if (input.getBytes() == null) {
            return null
        }

        if (input.getMessageConvertMethod() == MessageConvertMethod.DISABLED) {
            return null
        }

        if (input.getMimeType() != null) {
            if (serializableMessageConverter.supports(input.getMimeType())) {
                if (enableSerializableConverter) {
                    try {
                        ByteToObjectResult result = attemptConversion(serializableMessageConverter, input)

                        if (result != null) {
                            if (!input.getClassFilter() || input.getClassFilter().any() {
                                ClassUtils.isAssignableValue(it, result.getResult())
                            }) {
                                return result
                            }
                        }
                    }
                    catch (Exception ignored) {
                        // noop
                    }
                }
            }


            for (ByteToObjectConverter converter : getByteToObjectConverters()) {
                if (!converter.supports(input.getMimeType())) {
                    continue
                }

                if (!isInputAndConverterCompatible(input, converter)) {
                    continue
                }

                ByteToObjectResult result = attemptConversion(converter, input)

                if (result != null) {
                    return result
                }
            }
        }

        if (input.getMessageConvertMethod() != MessageConvertMethod.HEADER) {
            if (enableSerializableConverter) {
                try {
                    ByteToObjectResult result = attemptConversion(serializableMessageConverter, input)

                    if (result != null) {
                        if (!input.getClassFilter() || input.getClassFilter().any() {
                            ClassUtils.isAssignableValue(it, result.getResult())
                        }) {
                            return result
                        }
                    }
                }
                catch (Exception ignored) {
                    // noop
                }
            }

            for (ByteToObjectConverter converter : getByteToObjectConverters()) {
                if (!isInputAndConverterCompatible(input, converter)) {
                    continue
                }

                ByteToObjectResult result = attemptConversion(converter, input)

                if (result != null) {
                    return result
                }
            }
        }

        if (input.getClassFilter().contains(byte[])) {
            return new ByteToObjectResult(input.getBytes())
        }

        throw new NoConverterFoundException('no message converter found to convert a message body from a byte array')
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void register(MessageConverter messageConverter) {
        log.debug("Registering message consumer: ${messageConverter.getClass().getSimpleName()}")
        messageConverters << messageConverter
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void register(GrailsClass artefact) {
        register(applicationContext.getBean(artefact.propertyName, MessageConverter))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void reset() {
        messageConverters.clear()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void load() {
        grailsApplication.getArtefacts('MessageConverter').each { register((GrailsClass) it) }
        register(new TypeConvertingMapMessageConverter())
        register(new JsonMessageConverter())
        register(new LongMessageConverter())
        register(new IntegerMessageConverter())
        register(new StringMessageConverter())
    }

    /**
     * Attempts to convert some object to a <pre>byte[]</pre> with the given {@link ObjectToByteConverter}.
     *
     * @param messageConverter
     * @param rabbitMessageProperties
     * @return
     */
    protected ObjectToByteResult attemptConversion(ObjectToByteConverter messageConverter, ObjectToByteInput input) {
        try {
            return messageConverter.convert(input)
        }
        catch (Throwable e) {
            log.error("unhandled exception caught from message converter ${messageConverter.class.simpleName}", e)
            return null
        }
    }

    /**
     * Attempts to convert a <pre>byte[]</pre> to some object with the given {@link ByteToObjectConverter}.
     *
     * @param messageConverter
     * @param bytes
     * @return
     */
    protected ByteToObjectResult attemptConversion(ByteToObjectConverter messageConverter, ByteToObjectInput input) {
        try {
            return messageConverter.convert(input)
        }
        catch (Throwable e) {
            log.error("unhandled exception caught from message converter ${messageConverter.class.simpleName}", e)
            return null
        }
    }

    /**
     * Determines whether the given converter supports the given byte-to-object input.
     *
     * @param input
     * @param converter
     * @return
     */
    protected boolean isInputAndConverterCompatible(ByteToObjectInput input, ByteToObjectConverter converter) {
        if (!input.getClassFilter()) {
            return true
        }

        return input.getClassFilter().any { converter.supports(it) }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void setConfiguration(Config co) {
        enableSerializableConverter = co.getProperty('rabbitmq.enableSerializableConverter', Boolean, enableSerializableConverter)
    }
}
