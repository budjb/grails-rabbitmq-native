/*
 * Copyright 2017 Bud Byrd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.budjb.rabbitmq.test.publisher

import com.budjb.rabbitmq.connection.ConnectionManager
import com.budjb.rabbitmq.consumer.MessageContext
import com.budjb.rabbitmq.converter.*
import com.budjb.rabbitmq.exception.ContextNotFoundException
import com.budjb.rabbitmq.exception.NoConverterFoundException
import com.budjb.rabbitmq.publisher.RabbitMessageProperties
import com.budjb.rabbitmq.publisher.RabbitMessagePublisherImpl
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.impl.AMQImpl.Queue.DeclareOk
import spock.lang.Ignore
import spock.lang.Specification

import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeoutException

class RabbitMessagePublisherImplSpec extends Specification {
    private static final String BASIC_PUBLISH_MESSAGE = 'Knock knock...'
    private static final String BASIC_RESPONSE_MESSAGE = 'Who\'s there?'
    private static final String BASIC_PUBLISH_EXCHANGE = 'test-exchange'
    private static final String BASIC_PUBLISH_ROUTING_KEY = 'test-routing-key'

    ConnectionManager connectionManager
    RabbitMessagePublisherImpl rabbitMessagePublisher
    MessageConverterManager messageConverterManager
    Channel channel

    def mockBasicRpc(byte[] response) {
        // Mock temporary queue creation
        channel.queueDeclare() >> new DeclareOk('temporary-queue', 0, 0)

        // Mock a sync queue for the rpc consumer
        SynchronousQueue<MessageContext> queue = Mock(SynchronousQueue)

        // Set up the publisher as a spy (we need partial mocking for rpc calls)
        rabbitMessagePublisher = Spy(RabbitMessagePublisherImpl)
        rabbitMessagePublisher.createResponseQueue() >> queue
        rabbitMessagePublisher.connectionManager = connectionManager
        rabbitMessagePublisher.messageConverterManager = messageConverterManager

        // Create a mocked response message context
        MessageContext responseMessageContext = new MessageContext(
            channel: null,
            consumerTag: null,
            envelope: null,
            properties: new AMQP.BasicProperties(),
            body: response
        )

        // Mock the poll
        queue.poll(*_) >> responseMessageContext
        queue.take() >> responseMessageContext

    }

    @Ignore
    byte[] serialize(Serializable serializable) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        ObjectOutputStream oos = new ObjectOutputStream(baos)
        oos.writeObject(serializable)
        return baos.toByteArray()
    }

    def setup() {
        channel = Mock(Channel)

        connectionManager = Mock(ConnectionManager)
        connectionManager.createChannel(null) >> channel

        messageConverterManager = new MessageConverterManagerImpl()
        messageConverterManager.register(new LongMessageConverter())
        messageConverterManager.register(new JsonMessageConverter())
        messageConverterManager.register(new StringMessageConverter())

        rabbitMessagePublisher = new RabbitMessagePublisherImpl()
        rabbitMessagePublisher.connectionManager = connectionManager
        rabbitMessagePublisher.messageConverterManager = messageConverterManager
    }

    def 'Ensure setMessageConverterManager(MessageConverterManager) sets the property correctly'() {
        setup:
        MessageConverterManager messageConverterManager = Mock(MessageConverterManager)

        when:
        rabbitMessagePublisher.setMessageConverterManager(messageConverterManager)

        then:
        rabbitMessagePublisher.messageConverterManager == messageConverterManager
    }

    def 'Ensure setConnectionManager(ConnectionManager) sets the property correctly'() {
        setup:
        ConnectionManager connectionManager = Mock(ConnectionManager)

        when:
        rabbitMessagePublisher.setConnectionManager(connectionManager)

        then:
        rabbitMessagePublisher.connectionManager == connectionManager
    }

    def 'Basic send() with only a routing key'() {
        when:
        rabbitMessagePublisher.send(BASIC_PUBLISH_ROUTING_KEY, BASIC_PUBLISH_MESSAGE)

        then:
        1 * channel.basicPublish('', BASIC_PUBLISH_ROUTING_KEY, _, serialize(BASIC_PUBLISH_MESSAGE))
    }

    def 'Basic send() with an exchange and routing key'() {
        when:
        rabbitMessagePublisher.send(BASIC_PUBLISH_EXCHANGE, BASIC_PUBLISH_ROUTING_KEY, BASIC_PUBLISH_MESSAGE)

        then:
        1 * channel.basicPublish(BASIC_PUBLISH_EXCHANGE, BASIC_PUBLISH_ROUTING_KEY, _, serialize(BASIC_PUBLISH_MESSAGE))
    }

    def 'Basic send() with a provided RabbitMessageProperties object'() {
        when:
        rabbitMessagePublisher.send(new RabbitMessageProperties().build {
            exchange = BASIC_PUBLISH_EXCHANGE
            routingKey = BASIC_PUBLISH_ROUTING_KEY
            body = BASIC_PUBLISH_MESSAGE
        })

        then:
        1 * channel.basicPublish(BASIC_PUBLISH_EXCHANGE, BASIC_PUBLISH_ROUTING_KEY, _, serialize(BASIC_PUBLISH_MESSAGE))
    }

    def 'Basic send() configured by a closure'() {
        when:
        rabbitMessagePublisher.send {
            exchange = BASIC_PUBLISH_EXCHANGE
            routingKey = BASIC_PUBLISH_ROUTING_KEY
            body = BASIC_PUBLISH_MESSAGE
        }

        then:
        1 * channel.basicPublish(BASIC_PUBLISH_EXCHANGE, BASIC_PUBLISH_ROUTING_KEY, _, serialize(BASIC_PUBLISH_MESSAGE))
    }

    def 'Send with no parameters provided (routing key and/or exchange are required)'() {
        when:
        rabbitMessagePublisher.send {}

        then:
        thrown IllegalArgumentException
    }

    def 'If a channel is provided ensure one\'s not created and it\'s not closed'() {
        setup:
        Channel channel = Mock(Channel)

        when:
        rabbitMessagePublisher.send {
            routingKey = BASIC_PUBLISH_ROUTING_KEY
            delegate.channel = channel
        }

        then:
        0 * connectionManager.createChannel()
        0 * channel.close()
    }

    def 'If no channel is provided, ensure one\'s created and closed'() {
        when:
        rabbitMessagePublisher.send {
            routingKey = BASIC_PUBLISH_ROUTING_KEY
            body = 'asdf'
        }

        then:
        1 * connectionManager.createChannel(null) >> channel
        1 * channel.close()
    }

    def 'Ensure an exception is thrown when content can\'t be marshaled'() {
        when:
        rabbitMessagePublisher.send(BASIC_PUBLISH_ROUTING_KEY, new Expando())

        then:
        thrown NoConverterFoundException
    }

    def 'RPC with only a routing key'() {
        setup:
        mockBasicRpc(BASIC_RESPONSE_MESSAGE.getBytes())

        when:
        String response = rabbitMessagePublisher.rpc(BASIC_PUBLISH_ROUTING_KEY, BASIC_PUBLISH_MESSAGE)

        then:
        response == BASIC_RESPONSE_MESSAGE
        1 * channel.basicPublish('', BASIC_PUBLISH_ROUTING_KEY, _, serialize(BASIC_PUBLISH_MESSAGE))
    }

    def 'RPC with an exchange and routing key'() {
        setup:
        mockBasicRpc(BASIC_RESPONSE_MESSAGE.getBytes())

        when:
        String response = rabbitMessagePublisher.rpc(BASIC_PUBLISH_EXCHANGE, BASIC_PUBLISH_ROUTING_KEY, BASIC_PUBLISH_MESSAGE)

        then:
        response == BASIC_RESPONSE_MESSAGE
        1 * channel.basicPublish(BASIC_PUBLISH_EXCHANGE, BASIC_PUBLISH_ROUTING_KEY, _, serialize(BASIC_PUBLISH_MESSAGE))
    }

    def 'RPC call with a RabbitMessageProperties object'() {
        setup:
        mockBasicRpc(BASIC_RESPONSE_MESSAGE.getBytes())

        when:
        String response = rabbitMessagePublisher.rpc(new RabbitMessageProperties().build {
            exchange = BASIC_PUBLISH_EXCHANGE
            routingKey = BASIC_PUBLISH_ROUTING_KEY
            body = BASIC_PUBLISH_MESSAGE
        })

        then:
        response == BASIC_RESPONSE_MESSAGE
        1 * channel.basicPublish(BASIC_PUBLISH_EXCHANGE, BASIC_PUBLISH_ROUTING_KEY, _, serialize(BASIC_PUBLISH_MESSAGE))
    }

    def 'RPC call configured by a closure'() {
        setup:
        mockBasicRpc(BASIC_RESPONSE_MESSAGE.getBytes())

        when:
        String response = rabbitMessagePublisher.rpc {
            exchange = BASIC_PUBLISH_EXCHANGE
            routingKey = BASIC_PUBLISH_ROUTING_KEY
            body = BASIC_PUBLISH_MESSAGE
        }

        then:
        response == BASIC_RESPONSE_MESSAGE
        1 * channel.basicPublish(BASIC_PUBLISH_EXCHANGE, BASIC_PUBLISH_ROUTING_KEY, _, serialize(BASIC_PUBLISH_MESSAGE))
    }

    def 'Ensure that an RPC timeout throws an exception'() {
        setup:
        channel.queueDeclare() >> new DeclareOk('temporary-queue', 0, 0)
        SynchronousQueue<MessageContext> queue = new SynchronousQueue<MessageContext>()
        rabbitMessagePublisher = Spy(RabbitMessagePublisherImpl)
        rabbitMessagePublisher.createResponseQueue() >> queue
        rabbitMessagePublisher.connectionManager = connectionManager
        rabbitMessagePublisher.messageConverterManager = messageConverterManager

        when:
        rabbitMessagePublisher.rpc {
            routingKey = BASIC_PUBLISH_ROUTING_KEY
            timeout = 500
        }

        then:
        thrown TimeoutException
    }

    def 'If a channel is provided, ensure it is not closed and another one is not created'() {
        setup:
        mockBasicRpc(BASIC_RESPONSE_MESSAGE.getBytes())
        Channel channel = Mock(Channel)
        channel.queueDeclare() >> new DeclareOk('temporary-queue', 0, 0)

        when:
        rabbitMessagePublisher.rpc {
            routingKey = BASIC_PUBLISH_ROUTING_KEY
            delegate.channel = channel
        }

        then:
        0 * connectionManager.createChannel(_)
        0 * channel.close()
    }

    def 'If no channel is provided, ensure one is created and closed'() {
        setup:
        mockBasicRpc(BASIC_RESPONSE_MESSAGE.getBytes())

        when:
        rabbitMessagePublisher.rpc {
            routingKey = BASIC_PUBLISH_ROUTING_KEY
        }

        then:
        1 * connectionManager.createChannel(null) >> channel
        1 * channel.close()
    }

    def 'Verify that an RPC call publishes a message, consumes from a queue, and cancels consuming'() {
        setup:
        mockBasicRpc(BASIC_RESPONSE_MESSAGE.getBytes())

        when:
        rabbitMessagePublisher.rpc {
            routingKey = BASIC_PUBLISH_ROUTING_KEY
        }

        then:
        1 * channel.basicPublish(*_)
        1 * channel.basicConsume(*_)
        1 * channel.basicCancel(*_)
    }

    def 'If batching messages with withChannel(Closure), only one channel from the default connection should be used'() {
        setup:
        Channel channel = Mock(Channel)
        connectionManager.createChannel() >> channel >> { return Mock(Channel) }

        when:
        rabbitMessagePublisher.withChannel {
            5.times {
                send {
                    routingKey = 'test-queue'
                    body = 'hi'
                }
            }
        }

        then:
        5 * channel.basicPublish('', 'test-queue', _, serialize('hi'))
    }

    def 'If batching messages with withChannel(String, Closure), only one channel from the default connection should be used'() {
        setup:
        Channel channel = Mock(Channel)

        connectionManager.createChannel() >> { throw new ContextNotFoundException() }
        connectionManager.createChannel('connection1') >> channel >> Mock(Channel)
        connectionManager.createChannel(_) >> { throw new ContextNotFoundException() }

        when:
        rabbitMessagePublisher.withChannel('connection1') {
            5.times {
                send {
                    routingKey = 'test-queue'
                    body = 'hi'
                }
            }
        }

        then:
        5 * channel.basicPublish('', 'test-queue', _, serialize('hi'))
    }

    def 'Validate interactions for withConfirms(Closure)'() {
        setup:
        Channel channel = Mock(Channel)

        connectionManager.createChannel() >> channel >> Mock(Channel)
        connectionManager.createChannel(_) >> { throw new ContextNotFoundException() }

        when:
        rabbitMessagePublisher.withConfirms() {
            5.times {
                send {
                    routingKey = 'test-queue'
                    body = 'hi'
                }
            }
        }

        then:
        5 * channel.basicPublish('', 'test-queue', _, serialize('hi'))
        1 * channel.confirmSelect()
        1 * channel.waitForConfirms()
    }

    def 'Validate interactions for withConfirms(String, Closure)'() {
        setup:
        Channel channel = Mock(Channel)

        connectionManager.createChannel() >> { throw new ContextNotFoundException() }
        connectionManager.createChannel('connection1') >> channel >> Mock(Channel)
        connectionManager.createChannel(_) >> { throw new ContextNotFoundException() }

        when:
        rabbitMessagePublisher.withConfirms('connection1') {
            5.times {
                send {
                    routingKey = 'test-queue'
                    body = 'hi'
                }
            }
        }

        then:
        5 * channel.basicPublish('', 'test-queue', _, serialize('hi'))
        1 * channel.confirmSelect()
        1 * channel.waitForConfirms()
    }

    def 'Validate interactions for withConfirms(long, Closure)'() {
        setup:
        Channel channel = Mock(Channel)

        connectionManager.createChannel() >> channel >> Mock(Channel)
        connectionManager.createChannel(_) >> { throw new ContextNotFoundException() }

        when:
        rabbitMessagePublisher.withConfirms(5000) {
            5.times {
                send {
                    routingKey = 'test-queue'
                    body = 'hi'
                }
            }
        }

        then:
        5 * channel.basicPublish('', 'test-queue', _, serialize('hi'))
        1 * channel.confirmSelect()
        1 * channel.waitForConfirms(5000)
    }

    def 'Validate interactions for withConfirms(String, long, Closure)'() {
        setup:
        Channel channel = Mock(Channel)

        connectionManager.createChannel() >> { throw new ContextNotFoundException() }
        connectionManager.createChannel('connection1') >> channel >> Mock(Channel)
        connectionManager.createChannel(_) >> { throw new ContextNotFoundException() }

        when:
        rabbitMessagePublisher.withConfirms('connection1', 5000) {
            5.times {
                send {
                    routingKey = 'test-queue'
                    body = 'hi'
                }
            }
        }

        then:
        5 * channel.basicPublish('', 'test-queue', _, serialize('hi'))
        1 * channel.confirmSelect()
        1 * channel.waitForConfirms(5000)
    }

    def 'Validate interactions for withConfirmsOrDie(Closure)'() {
        setup:
        Channel channel = Mock(Channel)

        connectionManager.createChannel() >> channel >> Mock(Channel)
        connectionManager.createChannel(_) >> { throw new ContextNotFoundException() }

        when:
        rabbitMessagePublisher.withConfirmsOrDie() {
            5.times {
                send {
                    routingKey = 'test-queue'
                    body = 'hi'
                }
            }
        }

        then:
        5 * channel.basicPublish('', 'test-queue', _, serialize('hi'))
        1 * channel.confirmSelect()
        1 * channel.waitForConfirmsOrDie()
    }

    def 'Validate interactions for withConfirmsOrDie(String, Closure)'() {
        setup:
        Channel channel = Mock(Channel)

        connectionManager.createChannel() >> { throw new ContextNotFoundException() }
        connectionManager.createChannel('connection1') >> channel >> Mock(Channel)
        connectionManager.createChannel(_) >> { throw new ContextNotFoundException() }

        when:
        rabbitMessagePublisher.withConfirmsOrDie('connection1') {
            5.times {
                send {
                    routingKey = 'test-queue'
                    body = 'hi'
                }
            }
        }

        then:
        5 * channel.basicPublish('', 'test-queue', _, serialize('hi'))
        1 * channel.confirmSelect()
        1 * channel.waitForConfirmsOrDie()
    }

    def 'Validate interactions for withConfirmsOrDie(long, Closure)'() {
        setup:
        Channel channel = Mock(Channel)

        connectionManager.createChannel() >> channel >> Mock(Channel)
        connectionManager.createChannel(_) >> { throw new ContextNotFoundException() }

        when:
        rabbitMessagePublisher.withConfirmsOrDie(5000) {
            5.times {
                send {
                    routingKey = 'test-queue'
                    body = 'hi'
                }
            }
        }

        then:
        5 * channel.basicPublish('', 'test-queue', _, serialize('hi'))
        1 * channel.confirmSelect()
        1 * channel.waitForConfirmsOrDie(5000)
    }

    def 'Validate interactions for withConfirmsOrDie(String, long, Closure)'() {
        setup:
        Channel channel = Mock(Channel)

        connectionManager.createChannel() >> { throw new ContextNotFoundException() }
        connectionManager.createChannel('connection1') >> channel >> Mock(Channel)
        connectionManager.createChannel(_) >> { throw new ContextNotFoundException() }

        when:
        rabbitMessagePublisher.withConfirmsOrDie('connection1', 5000) {
            5.times {
                send {
                    routingKey = 'test-queue'
                    body = 'hi'
                }
            }
        }

        then:
        5 * channel.basicPublish('', 'test-queue', _, serialize('hi'))
        1 * channel.confirmSelect()
        1 * channel.waitForConfirmsOrDie(5000)
    }
}
