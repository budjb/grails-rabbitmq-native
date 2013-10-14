import com.budjb.rabbitmq.RabbitLoader
import com.budjb.rabbitmq.RabbitMessageBuilder

class RabbitmqNativeBootStrap {
    def grailsApplication

    def init = { servletContext ->
        // Inject the grailsApplication
        RabbitLoader.grailsApplication = grailsApplication
        RabbitMessageBuilder.grailsApplication = grailsApplication

        // Initialize the driver
        RabbitLoader.instance.start()

        // Start the listeners
        RabbitLoader.instance.startConsumers()
    }
}
