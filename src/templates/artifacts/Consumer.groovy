@artifact.package@import com.budjb.rabbitmq.MessageContext

@
import java.lang.Object

class @artifact.name@ {
    /**
     * Consumer configuration.
     */
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
    def handleMessage(def body, MessageContext context) {
        // TODO: Handle Message.
    }
}
