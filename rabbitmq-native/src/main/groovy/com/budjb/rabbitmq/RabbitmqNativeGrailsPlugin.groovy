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

import com.budjb.rabbitmq.artefact.MessageConsumerArtefactHandler
import com.budjb.rabbitmq.artefact.MessageConverterArtefactHandler
import com.budjb.rabbitmq.connection.ConnectionBuilderImpl
import com.budjb.rabbitmq.connection.ConnectionManagerImpl
import com.budjb.rabbitmq.consumer.ConsumerManagerImpl
import com.budjb.rabbitmq.converter.MessageConverterManagerImpl
import com.budjb.rabbitmq.publisher.RabbitMessagePublisherImpl
import com.budjb.rabbitmq.queuebuilder.QueueBuilderImpl
import grails.core.GrailsClass
import grails.plugins.Plugin
import groovy.util.logging.Slf4j

@Slf4j
class RabbitmqNativeGrailsPlugin extends Plugin {
    /**
     * The version or versions of Grails the plugin is designed for.
     */
    def grailsVersion = "3.0.0 > *"

    /**
     * Title/name of the plugin.
     */
    def title = "Rabbitmq Native Plugin"

    /**
     * Author's name.
     */
    def author = 'Bud Byrd'

    /**
     * Author email address.
     */
    def authorEmail = 'bud.byrd@gmail.com'

    /**
     * Description of the plugin.
     */
    def description = 'The native RabbitMQ Grails plugin provides easily consumable messaging functionality.'

    /**
     * URL to the plugin's documentation.
     */
    def documentation = "http://budjb.github.io/grails-rabbitmq-native/3.x/latest/"

    /**
     * Project license.
     */
    def license = "APACHE"

    /**
     * Location of the plugin's issue tracker.
     */
    def issueManagement = [system: 'GITHUB', url: 'https://github.com/budjb/grails-rabbitmq-native/issues']

    /**
     * Online location of the plugin's browseable source code.
     */
    def scm = [url: 'https://github.com/budjb/grails-rabbitmq-native']

    /**
     * Load order.
     */
    def loadAfter = ['controllers', 'services', 'domains', 'hibernate', 'spring-security-core']

    /**
     * Additional developers.
     */
    def developers = [
        [name: "Aaron Brown", email: "brown.aaron.lloyd@gmail.com"],
        [name: "Ollie Freeman", email: "ollie.freeman@gmail.com"]
    ]

    /**
     * Spring actions.
     *
     * @return
     */
    @Override
    Closure doWithSpring() {
        { ->
            // Create the null rabbit context bean
            'nullRabbitContext'(NullRabbitContext)

            // Create the live rabbit context bean
            'rabbitContextImpl'(RabbitContextImpl)

            // Create the proxy rabbit context bean
            'rabbitContext'(RabbitContextProxy) {
                if (!isRabbitEnabled()) {
                    log.warn("The rabbitmq-native plugin has been disabled by the application's configuration.")
                    target = ref('nullRabbitContext')
                }
                else {
                    target = ref('rabbitContextImpl')
                }
            }

            'rabbitLifecycleListener'(RabbitLifecycleListener)

            "connectionManager"(ConnectionManagerImpl)

            "connectionBuilder"(ConnectionBuilderImpl)

            "queueBuilder"(QueueBuilderImpl)

            "messageConverterManager"(MessageConverterManagerImpl)

            "consumerManager"(ConsumerManagerImpl)

            "rabbitMessagePublisher"(RabbitMessagePublisherImpl)

            grailsApplication.getArtefacts('MessageConverter').each { GrailsClass clazz ->
                "${clazz.propertyName}"(clazz.clazz) { bean ->
                    bean.autowire = true
                }
            }

            grailsApplication.getArtefacts('MessageConsumer').each { GrailsClass clazz ->
                "${clazz.fullName}"(clazz.clazz) { bean ->
                    bean.autowire = "byName"
                }
            }
        }
    }

    /**
     * Handle Grails service reloads.
     *
     * @param event
     */
    @Override
    void onChange(Map<String, Object> event) {
        if (!isRabbitEnabled()) {
            return
        }

        if (!event.source) {
            return
        }

        if (grailsApplication.isArtefactOfType(MessageConverterArtefactHandler.TYPE, event.source as Class<?>) ||
            grailsApplication.isArtefactOfType(MessageConsumerArtefactHandler.TYPE, event.source as Class<?>)
        ) {
            // TODO: change the reload to just re-register the resource
            applicationContext.getBean('rabbitContext', RabbitContext).reload()
        }
    }

    /**
     * Returns whether the plugin is enabled.
     *
     * @return
     */
    boolean isRabbitEnabled() {
        return config.getProperty('rabbitmq.enabled', Boolean, true)
    }
}
