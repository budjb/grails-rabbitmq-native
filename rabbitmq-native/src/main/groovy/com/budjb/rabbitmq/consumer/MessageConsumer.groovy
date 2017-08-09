package com.budjb.rabbitmq.consumer

import com.budjb.rabbitmq.exception.MissingConfigurationException
import grails.core.GrailsApplication
import grails.util.GrailsClassUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.ClassUtils

/**
 *
 */
abstract class MessageConsumer {
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
     * Consumer's configuration.
     */
    ConsumerConfiguration configuration

    /**
     * Called when a message has been received.
     *
     * @param messageContext
     */
    void onReceive(MessageContext messageContext) {

    }

    /**
     * Called when a message has completed processing successfully.
     *
     * @param messageContext
     */
    void onSuccess(MessageContext messageContext) {

    }

    /**
     * Called when an exception has occurred while processing a message.
     *
     * @param messageContext
     * @param throwable
     */
    void onFailure(MessageContext messageContext, Throwable throwable) {

    }

    /**
     * Called when processing of a message is complete. This will happen in
     * both the success and failure cases in addition to the success or
     * failure callbacks.
     *
     * @param messageContext
     */
    void onComplete(MessageContext messageContext) {

    }

    /**
     * Returns a list of all potential handler methods.
     *
     * @return
     */
    List<MetaMethod> findHandlers() {
        Object actualConsumer = getActualConsumer()

        List<MetaMethod> metaMethods = actualConsumer.getMetaClass().getMetaMethods().findAll {
            it.getName() == MESSAGE_HANDLER_NAME && it.getNativeParameterTypes().size() in [1, 2] && it.isPublic()
        }

        return metaMethods
    }

    /**
     * Finds a message handler method that will accept an incoming message.
     *
     * @param clazz Class type of the converted message body.
     * @return
     */
    MetaMethod findHandler(Class<?> clazz) {
        List<MetaMethod> metaMethods = findHandlers()

        MetaMethod match = metaMethods.find { isMetaMethodMatch(it, clazz) }

        if (!match) {
            match = metaMethods.find { isMetaMethodMatch(it, Object) }
        }

        if (!match) {
            match = metaMethods.find {
                it.getNativeParameterTypes().size() == 1 && ClassUtils.isAssignable(it.getNativeParameterTypes()[0], MessageContext)
            }
        }

        return match
    }

    /**
     * Returns the root consumer object. This is useful when a consumer object
     * is wrapped and does not directly extend this class.
     *
     * @return
     */
    protected Object getActualConsumer() {
        return this
    }

    /**
     * Determines if the given method is a match for the given message body type.
     *
     * @param method Method to check.
     * @param clazz Class of the converted message body type.
     * @return
     */
    protected boolean isMetaMethodMatch(MetaMethod method, Class<?> clazz) {
        Class[] parameters = method.getNativeParameterTypes()

        int parameterCount = parameters.size()

        if (parameterCount < 1 || parameterCount > 2) {
            return false
        }

        Class<?> first = parameters[0]

        if (first == Object.class && clazz != Object.class) {
            return false
        }

        if (!ClassUtils.isAssignable(first, clazz)) {
            return false
        }

        if (parameterCount == 2) {
            if (!ClassUtils.isAssignable(parameters[1], MessageContext)) {
                return false
            }
        }

        return true
    }

    /**
     * Returns the object types the consumer can convert to.
     *
     * @return
     */
    List<Class<?>> getSupportedConversionClasses() {
        return findHandlers().findAll { !MessageContext.isAssignableFrom(it.getNativeParameterTypes()[0]) }.collect {
            it.getNativeParameterTypes()[0]
        }
    }

    ConsumerConfiguration getConfiguration() {
        if (!this.configuration) {
            def config = GrailsClassUtils.getStaticPropertyValue(getActualConsumer().getClass(), RABBIT_CONFIG_NAME)
            if (config != null || Map.isInstance(config)) {
                this.configuration = new ConsumerConfigurationImpl((Map) config)
                return this.configuration
            }

            return config as Map

            Map configuration = loadConsumerLocalConfiguration(consumer) ?: loadConsumerApplicationConfiguration(consumer)
            if (!configuration) {
                throw new MissingConfigurationException("consumer has no configuration defined either within either its class or the application configuration")
            }

            return new ConsumerConfigurationImpl(configuration)
            getMetaClass().getMetaProperty('rabbitConfig')
        }
        return this.configuration
    }

    /**
     * Finds and returns a consumer's central configuration, or null if it isn't defined.
     *
     * @return
     */
    protected Map loadConsumerApplicationConfiguration(Object consumer) {
        def configuration = grailsApplication.config.rabbitmq.consumers."${consumer.getClass().simpleName}"

        if (!configuration || !Map.class.isAssignableFrom(configuration.getClass())) {
            return null
        }

        return configuration
    }

    /**
     * Finds and returns a consumer's local configuration, or null if it doesn't exist.
     *
     * @return
     */
    protected Map loadConsumerLocalConfiguration(Object consumer) {
        def config = GrailsClassUtils.getStaticPropertyValue(consumer.getClass(), RABBIT_CONFIG_NAME)

        if (config == null || !(config instanceof Map)) {
            return null
        }

        return config as Map
    }
}
