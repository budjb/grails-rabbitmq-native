import org.apache.log4j.Logger
import com.budjb.rabbitmq.RabbitContext
import com.budjb.rabbitmq.RabbitConsumer
import com.budjb.rabbitmq.MessageConverterArtefactHandler
import com.budjb.rabbitmq.converter.StringMessageConverter
import com.budjb.rabbitmq.GrailsMessageConverterClass
import org.codehaus.groovy.grails.commons.AbstractInjectableGrailsClass

class RabbitmqNativeGrailsPlugin {
    /**
     * Version of the plugin.
     */
    def version = "0.1"

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
    def description = 'The native RabbitMQ Grails plugin wraps RabbitMQ consumer funcionality around Grails services.'

    /**
     * URL to the plugin's documentation.
     */
    def documentation = "http://grails.org/plugin/rabbitmq-native"

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
    def scm = [url: 'https://github.com/budjb/grails-jersey-request-builder']

    /**
     * Load order.
     */
    def loadAfter = ['controllers', 'services', 'domains', 'hibernate', 'spring-security-core']

    /**
     * Resources this plugin should monitor changes for.
     */
    def watchedResources = [
        'file:./grails-app/services/**/*Service.groovy',
        'file:./grails-app/rabbit/**/*Converter.groovy',
        'file:./plugins/*/grails-app/rabbit/**/*Converter.groovy'
    ]

    /**
     * Custom artefacts
     */
    def artefacts = [
        new MessageConverterArtefactHandler()
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

        // Configure application-provided converters
        application.messageConverterClasses.each { GrailsMessageConverterClass grailsClass ->
            "${grailsClass.propertyName}"(grailsClass.clazz) { bean ->
                bean.scope = 'singleton'
                bean.autowire = true
            }
        }
    }

    /**
     * Application context actions.
     */
    def doWithApplicationContext = { applicationContext ->
        // Get the rabbit context instance
        RabbitContext context = applicationContext.getBean('rabbitContext')

        // Register built-in message converters to the rabbit context
        context.registerMessageConverter(applicationContext.getBean("${StringMessageConverter.name}"))

        // Register application-provided message converters to the rabbit context
        application.getArtefacts('MessageConverter').each { clazz ->
            context.registerMessageConverter(clazz.referenceInstance)
        }

        // TODO: register listeners

        // Completely restart rabbit context
        context.restart()
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

            // Reapply application context actions
            doWithApplicationContext(event.ctx)
            return
        }

        // Check for reloaded service listeners
        if (application.serviceClasses.any { it.clazz == event.source && RabbitConsumer.isConsumer(it) }) {
            event.ctx.getBean('rabbitContext').restartConsumers()
            return
        }
    }

    /**
     * Handle configuration changes.
     */
    def onConfigChange = { event ->
        event.ctx.getBean('rabbitContext').restart()
    }
}
