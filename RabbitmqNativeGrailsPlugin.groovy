import org.apache.log4j.Logger
import com.budjb.rabbitmq.RabbitContext
import com.budjb.rabbitmq.RabbitConsumer
import com.budjb.rabbitmq.MessageConverterArtefactHandler
import com.budjb.rabbitmq.converter.StringMessageConverter
import com.budjb.rabbitmq.GrailsMessageConverterClass
import org.codehaus.groovy.grails.commons.AbstractInjectableGrailsClass
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClass

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
            "${grailsClass.fullName}"(grailsClass.clazz) { bean ->
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
            restartRabbitContext(application, event.ctx.getBean('rabbitContext'))
            return
        }

        // Check for reloaded service consumers
        if (application.serviceClasses.any { it.clazz == event.source && RabbitConsumer.isConsumer(it) }) {
            restartConsumers(application, event.ctx.getBean('rabbitContext'))
            return
        }
    }

    /**
     * Handle configuration changes.
     */
    def onConfigChange = { event ->
        restartRabbitContext(application, event.ctx.getBean('rabbitContext'))
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

        // Start the consumers
        context.startConsumers()
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
        application.serviceClasses.each { GrailsClass clazz ->
            context.registerConsumer(clazz)
        }
    }

    /**
     * Registers message converters against the rabbit context.
     *
     * @param context
     */
    void registerConverters(GrailsApplication application, RabbitContext context) {
        // Register built-in message converters
        context.registerMessageConverter(application.mainContext.getBean("${StringMessageConverter.name}"))

        // Register application-provided converters
        application.messageConverterClasses.each { GrailsClass clazz ->
            context.registerMessageConverter(application.mainContext.getBean(clazz.fullName))
        }
    }
}
