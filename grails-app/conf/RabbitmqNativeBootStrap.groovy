import com.budjb.rabbitmq.RabbitContext
import com.budjb.rabbitmq.RabbitMessageBuilder

class RabbitmqNativeBootStrap {
    def grailsApplication

    def init = { servletContext ->
        // Inject the grailsApplication
        RabbitMessageBuilder.grailsApplication = grailsApplication
    }
}
