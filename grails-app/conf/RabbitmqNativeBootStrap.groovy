import com.budjb.rabbitmq.RabbitDriver

class RabbitmqNativeBootStrap {
    def grailsApplication

    def init = { servletContext ->
        // Inject the grailsApplication
        RabbitDriver.grailsApplication = grailsApplication

        // Initialize the driver
        RabbitDriver.instance.start()

        // Start the listeners
        RabbitDriver.instance.startConsumers()
    }
}
