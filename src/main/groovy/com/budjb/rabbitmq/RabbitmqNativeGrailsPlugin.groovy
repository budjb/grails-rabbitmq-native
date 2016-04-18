/*
 * Copyright 2013-2015 Bud Byrd
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

import com.budjb.rabbitmq.artefact.GrailsMessageConverterClass
import com.budjb.rabbitmq.artefact.MessageConsumerArtefactHandler
import com.budjb.rabbitmq.artefact.MessageConverterArtefactHandler
import com.budjb.rabbitmq.connection.ConnectionBuilderImpl
import com.budjb.rabbitmq.connection.ConnectionManagerImpl
import com.budjb.rabbitmq.consumer.ConsumerManagerImpl
import com.budjb.rabbitmq.converter.MessageConverterManagerImpl
import com.budjb.rabbitmq.publisher.RabbitMessagePublisherImpl
import grails.core.GrailsClass
import grails.plugins.Plugin
import org.apache.log4j.Logger

class RabbitmqNativeGrailsPlugin extends Plugin {

    /**
     * The version or versions of Grails the plugin is designed for.
     */
    def grailsVersion = "3.1.1 > *"

    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp",
            "grails-app/conf/application.groovy",
            '**/com/budjb/rabbitmq/test/**'

    ]

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
    def documentation = "http://budjb.github.io/grails-rabbitmq-native/doc/manual/guide/index.html"

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
     * Resources this plugin should monitor changes for.
     */
    def watchedResources = [
            'file:./grails-app/rabbit-converters/**Converter.groovy',
            'file:./grails-app/rabbit-consumers/**Consumer.groovy',
            'file:./plugins/*/grails-app/rabbit-converters/**Converter.groovy',
            'file:./plugins/*/grails-app/rabbit-consumers/**Consumer.groovy'
    ]

    /**
     * Logger.
     */
    Logger log = Logger.getLogger(RabbitmqNativeGrailsPlugin)

    def artefacts = [MessageConsumerArtefactHandler, MessageConverterArtefactHandler]

    /**
     * Spring actions.
     */
    Closure doWithSpring() {
        {->
            // Create the null rabbit context bean
            'nullRabbitContext'(NullRabbitContext)

            // Create the live rabbit context bean
            'rabbitContextImpl'(RabbitContextImpl)

            // Create the proxy rabbit context bean
            'rabbitContext'(RabbitContextProxy) {
                if (grailsApplication.config.rabbitmq.enabled == false) {
                    log.warn("The rabbitmq-native plugin has been disabled by the application's configuration.")
                    target = ref('nullRabbitContext')
                }
                else {
                    target = ref('rabbitContextImpl')
                }
            }

            "connectionManager"(ConnectionManagerImpl)

            "connectionBuilder"(ConnectionBuilderImpl)

            "queueBuilder"(QueueBuilderImpl)

            "messageConverterManager"(MessageConverterManagerImpl)

            "consumerManager"(ConsumerManagerImpl)

            "rabbitMessagePublisher"(RabbitMessagePublisherImpl)

            // Create application-provided converter beans
            grailsApplication.getArtefacts('MessageConverter').each {GrailsClass clazz ->
                "${clazz.propertyName}"(clazz.clazz) {bean ->
                    bean.autowire = true
                }
            }

            // Create consumer beans
            grailsApplication.getArtefacts('MessageConsumer').each {GrailsClass clazz ->
                "${clazz.propertyName}"(clazz.clazz) {bean ->
                    bean.autowire = true
                }
            }
        }
    }

    void doWithDynamicMethods() {
        // TODO Implement registering dynamic methods to classes (optional)
    }

    /**
     * Application context actions.
     */
    void doWithApplicationContext() {
        // Do nothing if the plugin's disabled.
        if (grailsApplication.config.rabbitmq.enabled != false) {

            // Load and start the rabbit service, without starting consumers.
            RabbitContext rabbitContext = (RabbitContext) applicationContext.getBean('rabbitContext')
            rabbitContext.load()
            rabbitContext.start()
        }
    }

    /**
     * Handle Grails service reloads.
     */
    void onChange(Map<String, Object> event) {
        // Do nothing if the plugin's disabled.
        if (grailsApplication.config.rabbitmq.enabled != false) {

            // Bail if no context
            if (!event.ctx) {
                return
            }

            // Check for reloaded message converters
            if (grailsApplication.isArtefactOfType(MessageConverterArtefactHandler.TYPE, event.source)) {
                // Re-register the bean
                GrailsMessageConverterClass converterClass = grailsApplication.
                        addArtefact(MessageConverterArtefactHandler.TYPE, event.source)
                beans {
                    "${converterClass.propertyName}"(converterClass.clazz) {bean ->
                        bean.autowire = true
                    }
                }

                // Restart the rabbit context
                RabbitContext context = event.ctx.getBean('rabbitContext')
                // TODO: change the reload to just re-register the message converter
                context.reload()
                return
            }

            // Check for reloaded message consumers
            if (grailsApplication.isArtefactOfType(MessageConsumerArtefactHandler.TYPE, event.source)) {
                // Re-register the bean
                GrailsMessageConverterClass consumerClass = grailsApplication.
                        addArtefact(MessageConsumerArtefactHandler.TYPE, event.source)
                beans {
                    "${consumerClass.propertyName}"(consumerClass.clazz) {bean ->
                        bean.autowire = true
                    }
                }

                // Restart the consumers
                RabbitContext context = event.ctx.getBean('rabbitContext')
                // TODO: change the reload to just re-register the message consumer
                context.reload()
            }
        }
    }

    /**
     * Handle configuration changes.
     */
    void onConfigChange(Map<String, Object> event) {
        if (grailsApplication.config.rabbitmq.enabled != false) {

            RabbitContext context = event.ctx.getBean('rabbitContext')
            context.reload()
        }
    }

    void onShutdown(Map<String, Object> event) {
        if (grailsApplication.config.rabbitmq.enabled != false) {

            RabbitContext context = event.ctx.getBean('rabbitContext')
            context.stop()
        }
    }
}
