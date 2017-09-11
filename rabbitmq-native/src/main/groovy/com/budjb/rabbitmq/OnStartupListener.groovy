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
            rabbitContext.start(!isAutoStart())
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

    /**
     * Returns whether the plugin is set to auto-start Consumers.
     *
     * @return
     */
    boolean isAutoStart() {
        def val = grailsApplication.config.rabbitmq.autoStart

        if (!(val instanceof Boolean)) {
            return true
        }

        return val
    }
}
