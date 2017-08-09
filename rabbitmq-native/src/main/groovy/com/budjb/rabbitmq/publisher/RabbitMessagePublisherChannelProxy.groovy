/*
 * Copyright 2016 Bud Byrd
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
package com.budjb.rabbitmq.publisher

import com.budjb.rabbitmq.consumer.MessageContext
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ShutdownSignalException

import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeoutException

class RabbitMessagePublisherChannelProxy implements RabbitMessagePublisher {
    /**
     * Rabbit message publisher.
     */
    RabbitMessagePublisher rabbitMessagePublisher

    /**
     * Channel to use for the duration of this object's lifetime.
     */
    Channel channel

    /**
     * Constructor.
     *
     * @param publisher
     * @param channel
     */
    RabbitMessagePublisherChannelProxy(RabbitMessagePublisher publisher, Channel channel) {
        this.rabbitMessagePublisher = publisher
        this.channel = channel
    }

    @Override
    void send(RabbitMessageProperties properties) throws IllegalArgumentException {
        properties.channel = channel

        rabbitMessagePublisher.send(properties)
    }

    @Override
    void send(@DelegatesTo(RabbitMessageProperties) Closure closure) throws IllegalArgumentException {
        send(createRabbitMessageProperties().build(closure))
    }

    @Override
    void send(String routingKey, Object body) throws IllegalArgumentException {
        send(createRabbitMessageProperties().build {
            delegate.routingKey = routingKey
            delegate.body = body
        })
    }

    @Override
    void send(String exchange, String routingKey, Object body) throws IllegalArgumentException {
        send(createRabbitMessageProperties().build {
            delegate.exchange = exchange
            delegate.routingKey = routingKey
            delegate.body = body
        })
    }

    @Override
    Object rpc(RabbitMessageProperties properties) throws TimeoutException, ShutdownSignalException, IOException, IllegalArgumentException {
        properties.channel = channel

        return rabbitMessagePublisher.rpc(properties)
    }

    @Override
    Object rpc(@DelegatesTo(RabbitMessageProperties) Closure closure)
        throws TimeoutException, ShutdownSignalException, IOException, IllegalArgumentException {
        return rpc(createRabbitMessageProperties().build(closure))
    }

    @Override
    Object rpc(String routingKey, Object body) throws TimeoutException, ShutdownSignalException, IOException, IllegalArgumentException {
        return rpc(createRabbitMessageProperties().build {
            delegate.routingKey = routingKey
            delegate.body = body
        })
    }

    @Override
    Object rpc(String exchange, String routingKey, Object body)
        throws TimeoutException, ShutdownSignalException, IOException, IllegalArgumentException {
        return rpc(createRabbitMessageProperties().build {
            delegate.exchange = exchange
            delegate.routingKey = routingKey
            delegate.body = body
        })
    }

    @Override
    SynchronousQueue<MessageContext> createResponseQueue() {
        throw new UnsupportedOperationException()
    }

    @Override
    void withConfirms(@DelegatesTo(RabbitMessagePublisher) Closure closure) {
        throw new UnsupportedOperationException()
    }

    @Override
    void withConfirms(long timeout, @DelegatesTo(RabbitMessagePublisher) Closure closure) {
        throw new UnsupportedOperationException()
    }

    @Override
    void withConfirmsOrDie(@DelegatesTo(RabbitMessagePublisher) Closure closure) {
        throw new UnsupportedOperationException()
    }

    @Override
    void withConfirmsOrDie(long timeout, @DelegatesTo(RabbitMessagePublisher) Closure closure) {
        throw new UnsupportedOperationException()
    }

    @Override
    void withChannel(@DelegatesTo(RabbitMessagePublisher) Closure closure) {
        throw new UnsupportedOperationException()
    }

    @Override
    void withChannel(String connection, @DelegatesTo(RabbitMessagePublisher) Closure closure) {
        throw new UnsupportedOperationException()
    }

    @Override
    void withConfirms(String connection, @DelegatesTo(RabbitMessagePublisher) Closure closure) {
        throw new UnsupportedOperationException()
    }

    @Override
    void withConfirms(String connection, long timeout, @DelegatesTo(RabbitMessagePublisher) Closure closure) {
        throw new UnsupportedOperationException()
    }

    @Override
    void withConfirmsOrDie(String connection, @DelegatesTo(RabbitMessagePublisher) Closure closure) {
        throw new UnsupportedOperationException()
    }

    @Override
    void withConfirmsOrDie(String connection, long timeout, @DelegatesTo(RabbitMessagePublisher) Closure closure) {
        throw new UnsupportedOperationException()
    }

    RabbitMessageProperties createRabbitMessageProperties() {
        return new RabbitMessageProperties()
    }

    void run(@DelegatesTo(RabbitMessagePublisherChannelProxy) Closure closure) {
        closure = closure.clone()
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = this
        closure(channel)
    }
}
