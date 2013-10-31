import com.budjb.rabbitmq.RabbitContext

class RabbitmqNativeBootStrap {
    RabbitContext rabbitContext

    def init = { servletContext ->
        rabbitContext.startConsumers()
    }
}
