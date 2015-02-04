/*
 * Copyright 2013-2014 Bud Byrd
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
import grails.util.Holders

import org.apache.log4j.Logger

import com.budjb.rabbitmq.RabbitContext
import com.budjb.rabbitmq.RabbitContextImpl
import com.budjb.rabbitmq.NullRabbitContext
import com.budjb.rabbitmq.MessageConverterArtefactHandler
import com.budjb.rabbitmq.MessageConsumerArtefactHandler
import com.budjb.rabbitmq.converter.*
import com.budjb.rabbitmq.GrailsMessageConverterClass
import com.budjb.rabbitmq.RabbitQueueBuilder

import org.codehaus.groovy.grails.commons.AbstractInjectableGrailsClass
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClass

class RabbitmqNativeGrailsPlugin {
    /**
     * Version of the plugin.
     */
    def version = "2.0.11"

    /**
     * The version or versions of Grails the plugin is designed for.
     */
    def grailsVersion = "2.0 > *"

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
     * Custom artefacts
     */
    def artefacts = [
        new MessageConverterArtefactHandler(),
        new MessageConsumerArtefactHandler()
    ]

    /**
     * Logger.
     */
    Logger log = Logger.getLogger('com.budjb.rabbitmq.RabbitmqNativeGrailsPlugin')

    /**
     * Spring actions.
     */
    def doWithSpring = {
        // Create the rabbit context bean
        Class rabbitContextClass
        if (application.config.rabbitmq.enabled == false) {
            // Set a null rabbitcontext object if the plugin is disabled
            rabbitContextClass = NullRabbitContext
            log.warn("The rabbitmq-native plugin has been disabled by the application's configuration.")
        }
        else {
            rabbitContextClass = ConnectedRabbitContext
        }
        "rabbitContext"(rabbitContextClass) { bean ->
            bean.scope = 'singleton'
            bean.autowire = true
        }

        // Create the built-in converter beans
        "${StringMessageConverter.name}"(StringMessageConverter)
        "${GStringMessageConverter.name}"(GStringMessageConverter)
        "${IntegerMessageConverter.name}"(IntegerMessageConverter)
        "${MapMessageConverter.name}"(MapMessageConverter)
        "${ListMessageConverter.name}"(ListMessageConverter)

        // Create application-provided converter beans
        application.messageConverterClasses.each { GrailsClass clazz ->
            "${clazz.fullName}"(clazz.clazz) { bean ->
                bean.scope = 'singleton'
                bean.autowire = true
            }
        }

        // Create consumer beans
        application.messageConsumerClasses.each { GrailsClass clazz ->
            "${clazz.fullName}"(clazz.clazz) { bean ->
                bean.scope = 'singleton'
                bean.autowire = true
            }
        }
    }

    /**
     * Application context actions.
     */
    def doWithApplicationContext = { applicationContext ->
        // Do nothing if the plugin's disabled.
        if (application.config.rabbitmq.enabled == false) {
            return
        }

        // Load and start the rabbit service, without starting consumers.
        RabbitContext rabbitContext = applicationContext.getBean('rabbitContext')
        rabbitContext.load()
        rabbitContext.start(true)
    }

    /**
     * Handle Grails service reloads.
     */
    def onChange = { event ->
        // Do nothing if the plugin's disabled.
        if (application.config.rabbitmq.enabled == false) {
            return
        }

        // Bail if no context
        if (!event.ctx) {
            return
        }

        // Check for reloaded message converters
        if (application.isArtefactOfType(MessageConverterArtefactHandler.TYPE, event.source)) {
            // Re-register the bean
            GrailsMessageConverterClass converterClass = application.addArtefact(MessageConverterArtefactHandler.TYPE, event.source)
            beans {
                "${converterClass.propertyName}"(converterClass.clazz) { bean ->
                    bean.scope = 'singleton'
                    bean.autowire = true
                }
            }.registerBeans(event.ctx)

            // Restart the rabbit context
            RabbitContext context = event.ctx.getBean('rabbitContext')
            context.restart()
            return
        }

        // Check for reloaded message consumers
        if (application.isArtefactOfType(MessageConsumerArtefactHandler.TYPE, event.source)) {
            // Re-register the bean
            GrailsMessageConverterClass consumerClass = application.addArtefact(MessageConsumerArtefactHandler.TYPE, event.source)
            beans {
                "${consumerClass.propertyName}"(consumerClass.clazz) { bean ->
                    bean.scope = 'singleton'
                    bean.autowire = true
                }
            }.registerBeans(event.ctx)

            // Restart the consumers
            RabbitContext context = event.ctx.getBean('rabbitContext')
            context.restart()
            return
        }
    }

    /**
     * Handle configuration changes.
     */
    def onConfigChange = { event ->
        // Do nothing if the plugin's disabled.
        if (application.config.rabbitmq.enabled == false) {
            return
        }

        RabbitContext context = event.ctx.getBean('rabbitContext')
        context.restart()
    }
}
