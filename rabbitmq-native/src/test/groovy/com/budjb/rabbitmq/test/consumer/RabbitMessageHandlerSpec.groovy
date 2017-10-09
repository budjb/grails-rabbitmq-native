package com.budjb.rabbitmq.test.consumer

import com.budjb.rabbitmq.connection.ConnectionContext
import com.budjb.rabbitmq.consumer.ConsumerContext
import com.budjb.rabbitmq.consumer.MessageContext
import com.budjb.rabbitmq.consumer.RabbitMessageHandler
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import org.slf4j.Logger
import spock.lang.Specification

class RabbitMessageHandlerSpec extends Specification {
    def 'When a throwable is encountered delivering a message within the consumer context handling logic, it is caught and logged'() {
        setup:
        Channel channel = Mock(Channel)
        ConsumerContext consumerContext = Mock(ConsumerContext)
        consumerContext.deliverMessage((MessageContext) _) >> { throw new RuntimeException() }
        ConnectionContext connectionContext = Mock(ConnectionContext)
        Logger log = Mock(Logger)

        RabbitMessageHandler rabbitMessageHandler = new RabbitMessageHandler(channel, 'asdf', consumerContext, connectionContext)
        rabbitMessageHandler.log = log

        when:
        rabbitMessageHandler.handleDelivery('asdf', Mock(Envelope), Mock(AMQP.BasicProperties), [] as byte[])

        then:
        notThrown RuntimeException
        1 * log.warn((String) _, { it instanceof RuntimeException })
    }
}
