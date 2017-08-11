package com.budjb.rabbitmq.consumer

import com.budjb.rabbitmq.converter.MessageConverterManager
import com.budjb.rabbitmq.exception.DuplicateHandlerException
import com.budjb.rabbitmq.exception.MessageConvertException
import com.budjb.rabbitmq.exception.MissingConfigurationException
import grails.core.GrailsApplication
import grails.util.GrailsClassUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.ClassUtils

class GrailsMessageConsumer extends BaseMessageConsumer implements InitializingBean {
    /**
     * Name of the method that should handle incoming messages.
     */
    protected static final String MESSAGE_HANDLER_NAME = 'handleMessage'

    /**
     * Name of the configuration variable a consumer is expected to define.
     */
    protected static final String RABBIT_CONFIG_NAME = 'rabbitConfig'

    /**
     * Grails application bean.
     */
    @Autowired
    GrailsApplication grailsApplication

    /**
     * Message converter manager bean.
     */
    @Autowired
    MessageConverterManager messageConverterManager

    /**
     * {@inheritDoc}
     */
    ConsumerConfiguration configuration

    /**
     * List of classes the handlers of this consumer supports.
     */
    protected Map<Class<?>, MetaMethod> handlers = [:]

    /**
     * Logger.
     */
    Logger log = LoggerFactory.getLogger(getClass())

    /**
     * {@inheritDoc}
     */
    Object process(MessageContext messageContext) {
        Object body
        try {
            body = convertMessage(messageContext)
        }
        catch (Throwable e) {
            log.error("unexpected exception ${e.getClass()} encountered converting incoming request with handler ${getId()}", e)
            return null
        }

        MetaMethod handler = findHandler(body)
        if (!handler) {
            handler = findHandler(messageContext.body)
        }
        if (!handler) {
            handler = findHandler(messageContext)
        }
        if (!handler) {
            throw new IllegalStateException("could not find a handler method for class type ${body.getClass()}")
        }

        if (handler.nativeParameterTypes.size() == 1) {
            if (MessageContext.isAssignableFrom(handler.nativeParameterTypes[0])) {
                return handler.invoke(getActualConsumer(), messageContext)
            }
            else {
                return handler.invoke(getActualConsumer(), body)
            }
        }
        else {
            return handler.invoke(getActualConsumer(), body, messageContext)
        }
    }

    /**
     * Attempts to convert the body of the incoming message from a byte array.
     * The output of this method is dependent on the consumer's configuration,
     * the content-type of the message, and the existence of an appropriately
     * defined handler for the converted type.
     *
     * @param context
     * @return
     */
    protected Object convertMessage(MessageContext context) {
        ConsumerConfiguration configuration = getConfiguration()
        List<Class<?>> supportedTypes = handlers.keySet().toList()

        if (configuration.getConvert() == MessageConvertMethod.DISABLED) {
            return context.getBody()
        }

        if (context.getProperties().getContentType()) {
            try {
                return messageConverterManager.convertFromBytes(context.getBody(), supportedTypes, context.getProperties().getContentType())
            }
            catch (MessageConvertException ignore) {
                // Continue
            }
        }

        if (configuration.getConvert() == MessageConvertMethod.HEADER) {
            return context.getBody()
        }

        try {
            return messageConverterManager.convertFromBytes(context.getBody(), supportedTypes.toList())
        }
        catch (MessageConvertException ignore) {
            // Continue
        }

        return context.getBody()
    }

    @Override
    void afterPropertiesSet() throws Exception {
        loadConfiguration()
        loadHandlers()
    }

    /**
     * Loads the consumer's configuration.
     */
    protected void loadConfiguration() {
        if (getConfiguration() != null) {
            return
        }

        def configuration = grailsApplication.config.rabbitmq.consumers."${getName()}"

        if (configuration && Map.isInstance(configuration)) {
            this.configuration = new ConsumerConfigurationImpl((Map) configuration)
            return
        }

        configuration = GrailsClassUtils.getStaticPropertyValue(getActualConsumer().getClass(), RABBIT_CONFIG_NAME)

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
        Object actualConsumer = getActualConsumer()

        for (MetaMethod method : actualConsumer.getMetaClass().getMethods()) {
            if (method.getName() == MESSAGE_HANDLER_NAME && method.isPublic()) {
                Class[] types = method.getNativeParameterTypes()

                if (types.size() == 1 || types.size() == 2) {
                    Class<?> type = types[0]

                    if (handlers.containsKey(type)) {
                        throw new DuplicateHandlerException(type)
                    }

                    handlers.put(type, method)
                }
            }
        }

        if (!handlers.size()) {
            // TODO: custom exception
            throw new Exception('TODO: no handlers defined')
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
    protected MetaMethod findHandler(Object object) {
        MetaMethod match = handlers.find { owner.isHandlerMatch(it.key, object.getClass()) }?.value

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
