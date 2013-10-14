import org.apache.log4j.Logger
import com.budjb.rabbitmq.RabbitLoader
import com.budjb.rabbitmq.RabbitConsumer

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
     * Resources this plugin should monitor changes for.
     */
    def watchedResources = 'file:./grails-app/services/*Service.groovy'

    /**
     * Logger.
     */
    Logger log = Logger.getLogger('com.budjb.rabbitmq.RabbitmqNativeGrailsPlugin')

    /**
     * Handle Grails service reloads.
     */
    def onChange = { event ->
        if (application.serviceClasses.any { it.clazz == event.source && RabbitConsumer.isConsumer(it) }) {
            RabbitLoader.instance.restartConsumers()
        }
    }

    /**
     * Handle configuration changes.
     */
    def onConfigChange = { event ->
        RabbitLoader.instance.restart()
    }

    /**
     * Shutdown event.
     */
    def onShutdown = { event ->
        RabbitLoader.instance.stop()
    }
}
