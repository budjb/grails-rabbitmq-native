package com.budjb.rabbitmq.test.support

import com.budjb.rabbitmq.consumer.MessageConsumer
import com.budjb.rabbitmq.consumer.MessageConsumerEventHandler

interface CompositeMessageConsumer extends MessageConsumer, MessageConsumerEventHandler{
}
