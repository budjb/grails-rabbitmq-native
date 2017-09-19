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

import com.budjb.rabbitmq.converter.ByteToObjectInput
import com.budjb.rabbitmq.converter.MessageConverterManager
import com.budjb.rabbitmq.exception.DuplicateHandlerException
import com.budjb.rabbitmq.exception.MissingConfigurationException
import com.budjb.rabbitmq.exception.NoConverterFoundException
import com.budjb.rabbitmq.exception.NoMessageHandlersDefinedException
import grails.config.Config
import grails.util.GrailsClassUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.util.ClassUtils

import java.lang.reflect.Method
import java.lang.reflect.Modifier

class LegacyMessageConsumer implements MessageConsumer, MessageConsumerEventHandler {
    /**
     * Name of the method that should handle incoming messages.
     */
    protected static final String MESSAGE_HANDLER_NAME = 'handleMessage'

    /**
     * Name of the configuration variable a consumer is expected to define.
     */
    protected static final String RABBIT_CONFIG_NAME = 'rabbitConfig'

    /**
     * {@inheritDoc}
     */
    ConsumerConfiguration configuration

    /**
     * List of classes the handlers of this consumer supports.
     */
    protected Map<Class<?>, Method> handlers = [:]

    /**
     * Wrapped consumer object.
     */
    protected final Object consumer

    /**
     * Grails configuration.
     */
    protected final Config grailsConfig

    /**
     * Message converter manager bean.
     */
    protected final MessageConverterManager messageConverterManager

    /**
     * Logger.
     */
    Logger log = LoggerFactory.getLogger(getClass())

    /**
     * Constructor.
     *
     * @param consumer Consumer object to wrap.
     * @param grailsConfig Grails configuration.
     * @param messageConverterManager Message converter manager instance.
     */
    LegacyMessageConsumer(Object consumer, Config grailsConfig, MessageConverterManager messageConverterManager) {
        this.consumer = consumer
        this.grailsConfig = grailsConfig
        this.messageConverterManager = messageConverterManager

        loadConfiguration()
        loadHandlers()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String getId() {
        return consumer.getClass().getName()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String getName() {
        return consumer.getClass().getSimpleName()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void onReceive(MessageContext messageContext) {
        if (MessageConsumerEventHandler.isInstance(consumer)) {
            ((MessageConsumerEventHandler) consumer).onReceive(messageContext)
        }
        else {
            MetaMethod method = consumer.getMetaClass().getMethods().find { it.name == 'onReceive' }

            if (method) {
                try {
                    method.checkParameters([MessageContext] as Class[])
                    method.invoke(consumer, messageContext)
                }
                catch (IllegalArgumentException ignored) {
                    log.error("consumer ${getId()} has an invalid onReceive method signature")
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void onSuccess(MessageContext messageContext) {
        if (MessageConsumerEventHandler.isInstance(consumer)) {
            ((MessageConsumerEventHandler) consumer).onSuccess(messageContext)
        }
        else {
            MetaMethod method = consumer.getMetaClass().getMethods().find { it.name == 'onSuccess' }

            if (method) {
                try {
                    method.checkParameters([MessageContext] as Class[])
                    method.invoke(consumer, messageContext)
                }
                catch (IllegalArgumentException ignored) {
                    log.error("consumer ${getId()} has an invalid onSuccess method signature")
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void onFailure(MessageContext messageContext, Throwable throwable) {
        if (MessageConsumerEventHandler.isInstance(consumer)) {
            ((MessageConsumerEventHandler) consumer).onFailure(messageContext, throwable)
        }
        else {
            MetaMethod method = consumer.getMetaClass().getMethods().find { it.name == 'onFailure' }

            if (method) {
                try {
                    method.checkParameters([MessageContext, Throwable] as Class[])
                    method.invoke(consumer, messageContext, throwable)
                }
                catch (IllegalArgumentException ignored) {
                    log.error("consumer ${getId()} has an invalid onFailure method signature")
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void onComplete(MessageContext messageContext) {
        if (MessageConsumerEventHandler.isInstance(consumer)) {
            ((MessageConsumerEventHandler) consumer).onComplete(messageContext)
        }
        else {
            MetaMethod method = consumer.getMetaClass().getMethods().find { it.name == 'onComplete' }

            if (method) {
                try {
                    method.checkParameters([MessageContext] as Class[])
                    method.invoke(consumer, messageContext)
                }
                catch (IllegalArgumentException ignored) {
                    log.error("consumer ${getId()} has an invalid onComplete method signature")
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    Object process(MessageContext messageContext) {
        Method handler = null
        Object body = null

        if (messageContext.getBody() != null) {
            try {
                body = messageConverterManager.convert(new ByteToObjectInput(
                    messageContext.getBody(),
                    (String) messageContext.getProperties().getContentType(),
                    getConfiguration().getConvert(),
                    handlers.keySet().toList())
                ).getResult()
            }
            catch (NoConverterFoundException ignored) {
                // There was no message converter that could convert the body.
                // This is OK. The consumer may have a byte[] or MessageContext handler.
            }
            catch (Throwable e) {
                throw new RuntimeException("unexpected exception ${e.getClass()} encountered converting incoming request with handler ${getId()}", e)
            }
        }

        if (body != null) {
            handler = findHandler(body)
        }

        if (!handler && messageContext.getBody() != null) {
            handler = findHandler(messageContext.getBody())
        }

        if (!handler) {
            handler = findHandler(messageContext)
        }

        if (!handler) {
            throw new IllegalArgumentException("could not find a message converter and message handler combination to process an incoming message")
        }

        if (handler.getParameterCount() == 1) {
            if (ClassUtils.isAssignable(MessageContext, handler.getParameterTypes()[0])) {
                return handler.invoke(consumer, [messageContext] as Object[])
            }
            else {
                return handler.invoke(consumer, [body] as Object[])
            }
        }
        else {
            return handler.invoke(consumer, body, messageContext)
        }
    }

    /**
     * Loads the consumer's configuration.
     */
    protected void loadConfiguration() {
        if (getConfiguration() != null) {
            return
        }

        def configuration = grailsConfig.getProperty("rabbitmq.consumers.${getName()}", Map)

        if (configuration) {
            this.configuration = new ConsumerConfigurationImpl((Map) configuration)
            return
        }

        configuration = GrailsClassUtils.getStaticPropertyValue(consumer.getClass(), RABBIT_CONFIG_NAME)

        if (configuration && Map.isInstance(configuration)) {
            this.configuration = new ConsumerConfigurationImpl((Map) configuration)
            return
        }

        throw new MissingConfigurationException("consumer has no configuration defined either within either its class or the application configuration")
    }

    /**
     * Returns a list of all potential handler methods.
     */
    protected void loadHandlers() {
        for (Method method : consumer.getClass().getMethods()) {
            if (method.getName() == MESSAGE_HANDLER_NAME && Modifier.isPublic(method.getModifiers())) {
                Class[] types = method.getParameterTypes()

                if (types.size() == 1 || types.size() == 2) {
                    Class<?> type = types[0]

                    if (handlers.containsKey(type)) {
                        throw new DuplicateHandlerException(type)
                    }

                    handlers.put(types[0], method)
                }
            }
        }

        if (!handlers.size()) {
            throw new NoMessageHandlersDefinedException(consumer.getClass())
        }
    }

    /**
     * Finds a message handler method that will accept an incoming message.
     *
     * This method will prioritize method signatures that do not include the
     * Object type, since any class will match that signature if present.
     *
     * @param object Object to find a handler for.
     * @return
     */
    protected Method findHandler(Object object) {
        Method match = handlers.find { owner.isHandlerMatch(it.key, object.getClass()) }?.value

        if (!match) {
            match = handlers.find { owner.isHandlerMatch(it.key, Object) }?.value
        }

        return match
    }

    /**
     * Determines if the given method is a match for the given message body type.
     *
     * @param method Method to check.
     * @param clazz Class of the converted message body type.
     * @return
     */
    protected boolean isHandlerMatch(Class<?> handler, Class<?> clazz) {
        if (handler == Object.class && clazz != Object.class) {
            return false
        }

        if (!ClassUtils.isAssignable(handler, clazz)) {
            return false
        }

        return true
    }
}
