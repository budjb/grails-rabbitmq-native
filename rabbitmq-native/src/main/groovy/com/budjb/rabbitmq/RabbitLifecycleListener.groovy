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
package com.budjb.rabbitmq

import grails.config.Config
import grails.core.GrailsApplicationLifeCycleAdapter
import grails.core.support.GrailsConfigurationAware
import org.springframework.beans.factory.annotation.Autowired

class RabbitLifecycleListener extends GrailsApplicationLifeCycleAdapter implements GrailsConfigurationAware {
    /**
     * Grails application bean.
     */
    Config configuration

    /**
     * Rabbit context.
     */
    @Autowired
    RabbitContext rabbitContext

    /**
     * {@inheritDoc}
     */
    @Override
    void doWithApplicationContext() {
        if (isEnabled()) {
            rabbitContext.load()
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void onStartup(Map<String, Object> event) {
        if (isEnabled() && isAutoStart()) {
            rabbitContext.start()
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void onConfigChange(Map<String, Object> event) {
        if (isEnabled()) {
            rabbitContext.reload()
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void onShutdown(Map<String, Object> event) {
        if (isEnabled()) {
            rabbitContext.stop()
        }
    }

    /**
     * Returns whether the plugin is enabled.
     *
     * @return
     */
    boolean isEnabled() {
        return configuration.getProperty('rabbitmq.enabled', Boolean, true)
    }

    /**
     * Returns whether the plugin is set to auto-start Consumers.
     *
     * @return
     */
    boolean isAutoStart() {
        return configuration.getProperty('rabbitmq.autoStart', Boolean, true)
    }
}
