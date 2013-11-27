/*
 * Copyright 2013 Bud Byrd
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
import com.budjb.rabbitmq.RabbitConsumer
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
    def version = "0.2.0"

    /**
     * Maven group.
     */
    def group = 'com.rackspace.rvi'

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
     * Rabbit context name.
     */

    /**
     * Spring actions.
     */
    def doWithSpring = {
        // Setup the rabbit context
        "rabbitContext"(RabbitContext) { bean ->
            bean.scope = 'singleton'
            bean.autowire = true
        }

        // Configure built-in converters
        "${StringMessageConverter.name}"(StringMessageConverter)
        "${IntegerMessageConverter.name}"(IntegerMessageConverter)
        "${MapMessageConverter.name}"(MapMessageConverter)
        "${ListMessageConverter.name}"(ListMessageConverter)

        // Configure application-provided converters
        application.messageConverterClasses.each { GrailsClass clazz ->
            "${clazz.fullName}"(clazz.clazz) { bean ->
                bean.scope = 'singleton'
                bean.autowire = true
            }
        }

        // Configure consumers
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
        restartRabbitContext(application, applicationContext.getBean('rabbitContext'))
    }

    /**
     * Handle Grails service reloads.
     */
    def onChange = { event ->
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
            restartRabbitContext(application, context)
            context.startConsumers()
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
            restartConsumers(application, event.ctx.getBean('rabbitContext'))
            return
        }
    }

    /**
     * Handle configuration changes.
     */
    def onConfigChange = { event ->
        RabbitContext context = event.ctx.getBean('rabbitContext')
        restartRabbitContext(application, context)
        context.startConsumers()
    }

    /**
     * Restarts the rabbit context.
     *
     * @param context
     */
    void restartRabbitContext(GrailsApplication application, RabbitContext context) {
        // Stop the rabbit context
        context.stop()

        // Register message converters
        registerConverters(application, context)

        // Register consumers
        registerConsumers(application, context)

        // Start the rabbit context
        context.start()

        // Configure up exchanges and queues
        configureQueues(application, context)
    }

    /**
     * Configure queues based on the application's configuration.
     *
     * @param application
     */
    void configureQueues(GrailsApplication application, RabbitContext context) {
        // Skip if the config isn't defined
        if (!(application.config.rabbitmq?.queues instanceof Closure)) {
            return
        }

        // Grab the config closure
        Closure config = application.config.rabbitmq.queues

        // Create the queue builder
        RabbitQueueBuilder queueBuilder = new RabbitQueueBuilder(context)

        // Run the config
        config = config.clone()
        config.delegate = queueBuilder
        config.resolveStrategy = Closure.DELEGATE_FIRST
        config()
    }

    /**
     * Restarts the rabbit consumers.
     *
     * @param context
     */
    void restartConsumers(GrailsApplication application, RabbitContext context) {
        // Stop the consumers
        context.stopConsumers()

        // Register consumers again
        registerConsumers(application, context)

        // Start the consumers
        context.startConsumers()
    }

    /**
     * Registers consumers against the rabbit context.
     *
     * @param context
     */
    void registerConsumers(GrailsApplication application, RabbitContext context) {
        application.messageConsumerClasses.each { GrailsClass clazz ->
            context.registerConsumer(clazz)
        }
    }

    /**
     * Registers message converters against the rabbit context.
     *
     * @param context
     */
    void registerConverters(GrailsApplication application, RabbitContext context) {
        // Register application-provided converters
        application.messageConverterClasses.each { GrailsClass clazz ->
            context.registerMessageConverter(application.mainContext.getBean(clazz.fullName))
        }

        // Register built-in message converters
        // Note: the order matters, we want string to be the last one
        context.registerMessageConverter(application.mainContext.getBean("${IntegerMessageConverter.name}"))
        context.registerMessageConverter(application.mainContext.getBean("${MapMessageConverter.name}"))
        context.registerMessageConverter(application.mainContext.getBean("${ListMessageConverter.name}"))
        context.registerMessageConverter(application.mainContext.getBean("${StringMessageConverter.name}"))
    }
}
