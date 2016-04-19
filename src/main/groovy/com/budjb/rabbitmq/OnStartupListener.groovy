package com.budjb.rabbitmq

import grails.core.GrailsApplication
import grails.core.GrailsApplicationLifeCycleAdapter
import grails.core.support.GrailsApplicationAware
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

class OnStartupListener extends GrailsApplicationLifeCycleAdapter implements GrailsApplicationAware, ApplicationContextAware {
    /**
     * Grails application bean.
     */
    GrailsApplication grailsApplication

    /**
     * Application context.
     */
    ApplicationContext applicationContext

    /**
     * Application context actions.
     *
     * @param event
     */
    @Override
    void onStartup(Map<String, Object> event) {
        if (isEnabled()) {
            RabbitContext rabbitContext = getRabbitContextBean()
            rabbitContext.load()
            rabbitContext.start()
        }
    }

    /**
     * Return the RabbitContext bean.
     *
     * @return
     */
    RabbitContext getRabbitContextBean() {
        return applicationContext.getBean('rabbitContext', RabbitContext)
    }

    /**
     * Returns whether the plugin is enabled.
     *
     * @return
     */
    boolean isEnabled() {
        def val = grailsApplication.config.rabbitmq.enabled

        if (!(val instanceof Boolean)) {
            return true
        }

        return val
    }
}
