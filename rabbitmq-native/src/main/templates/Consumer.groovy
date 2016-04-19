package ${packageName}

import com.budjb.rabbitmq.consumer.MessageContext

class ${className}Consumer {

    static rabbitConfig = [
        : // TODO: Setup config.
    ]

    /**
     * Handle an incoming RabbitMQ message.
     *
     * @param body    The converted body of the incoming message.
     * @param context Properties of the incoming message.
     * @return
     */
    def handleMessage(def body, MessageContext messageContext) {
        // TODO: Handle messages
        body
    }
}