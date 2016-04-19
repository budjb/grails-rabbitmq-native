/*
 * Copyright 2015 Bud Byrd
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
import com.rabbitmq.client.ShutdownSignalException

import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeoutException

interface RabbitMessagePublisher {
    /**
     * Sends a Rabbit message with a given set of message properties.
     *
     * @param properties
     * @throws IllegalArgumentException
     */
    void send(RabbitMessageProperties properties) throws IllegalArgumentException

    /**
     * Builds a properties object with the given closure and sends a Rabbit message.
     *
     * @param closure
     * @throws IllegalArgumentException
     */
    void send(@DelegatesTo(RabbitMessageProperties) Closure closure) throws IllegalArgumentException

    /**
     * Sends a Rabbit message with a given routing key and payload.
     *
     * @param routingKey
     * @param body
     * @throws IllegalArgumentException
     */
    void send(String routingKey, Object body) throws IllegalArgumentException

    /**
     * Sends a rabbit message with a given exchange, routing key, and payload.
     *
     * @param exchange
     * @param routingKey
     * @param body
     * @throws IllegalArgumentException
     */
    void send(String exchange, String routingKey, Object body) throws IllegalArgumentException

    /**
     * Sends a message to the bus and waits for a reply, up to the "timeout" property.
     *
     * This method returns a Message object if autoConvert is set to false, or some
     * other object type (string, list, map) if autoConvert is true.
     *
     * The logic for the handler is based on the RPC handler found in spring's RabbitTemplate.
     *
     * @throws TimeoutException
     * @throws ShutdownSignalException
     * @throws IOException
     * @throws IllegalArgumentException
     * @return
     */
    Object rpc(RabbitMessageProperties properties) throws TimeoutException, ShutdownSignalException, IOException, IllegalArgumentException

    /**
     * Sends a message to the bus and waits for a reply, up to the "timeout" property.
     *
     * This method returns a Message object if autoConvert is set to false, or some
     * other object type (string, list, map) if autoConvert is true.
     *
     * @param closure
     * @return
     * @throws TimeoutException
     * @throws ShutdownSignalException
     * @throws IOException
     * @throws IllegalArgumentException
     */
    Object rpc(@DelegatesTo(RabbitMessageProperties) Closure closure)
            throws TimeoutException, ShutdownSignalException, IOException, IllegalArgumentException

    /**
     * Sends a message to the bus and waits for a reply, up to the "timeout" property.
     *
     * This method returns a Message object if autoConvert is set to false, or some
     * other object type (string, list, map) if autoConvert is true.
     *
     * @param routingKey Routing key to send the message to.
     * @param body Message payload.
     * @return
     * @throws TimeoutException
     * @throws ShutdownSignalException
     * @throws IOException
     * @throws IllegalArgumentException
     */
    Object rpc(String routingKey, Object body) throws TimeoutException, ShutdownSignalException, IOException, IllegalArgumentException

    /**
     * Sends a message to the bus and waits for a reply, up to the "timeout" property.
     *
     * This method returns a Message object if autoConvert is set to false, or some
     * other object type (string, list, map) if autoConvert is true.
     *
     * @param exchange Exchange to send the message to.
     * @param routingKey Routing key to send the message to.
     * @param body Message payload.
     * @return
     * @throws TimeoutException
     * @throws ShutdownSignalException
     * @throws IOException
     * @throws IllegalArgumentException
     */
    Object rpc(String exchange, String routingKey, Object body)
            throws TimeoutException, ShutdownSignalException, IOException, IllegalArgumentException

    /**
     * Creates and returns a synchronous queue for use in the RPC consumer.
     *
     * @return
     */
    SynchronousQueue<MessageContext> createResponseQueue()

    /**
     * Performs a series of operations with the same channel.
     *
     * @param Closure
     */
    void withChannel(@DelegatesTo(RabbitMessagePublisher) Closure closure)

    /**
     * Performs a series of operations with the same channel created from the given connection.
     *
     * @param connection
     * @param closure
     */
    void withChannel(String connection, @DelegatesTo(RabbitMessagePublisher) Closure closure)

    /**
     * Performs a series of operations with the same channel and with confirms enabled.
     *
     * @param closure
     */
    void withConfirms(@DelegatesTo(RabbitMessagePublisher) Closure closure)

    /**
     * Performs a series of operations with the same channel created from the given connection
     * and with confirms enabled.
     *
     * @param connection
     * @param closure
     */
    void withConfirms(String connection, @DelegatesTo(RabbitMessagePublisher) Closure closure)

    /**
     * Performs a series of operations with the same channel and with confirms enabled.
     *
     * @param timeout
     * @param closure
     */
    void withConfirms(long timeout, @DelegatesTo(RabbitMessagePublisher) Closure closure)

    /**
     * Performs a series of operations with the same channel created from the given connection
     * and with confirms enabled.
     * @param connection
     * @param timeout
     * @param closure
     */
    void withConfirms(String connection, long timeout, @DelegatesTo(RabbitMessagePublisher) Closure closure)

    /**
     * Performs a series of operations with the same channel and with confirms enabled.
     * This method will throw an exception if any messages are n'acked.
     *
     * @param closure
     */
    void withConfirmsOrDie(@DelegatesTo(RabbitMessagePublisher) Closure closure)

    /**
     * Performs a series of operations with the same channel created from the given connection
     * and with confirms enabled. This method will throw an exception if any messaged are n'acked.
     *
     * @param connection
     * @param closure
     */
    void withConfirmsOrDie(String connection, @DelegatesTo(RabbitMessagePublisher) Closure closure)

    /**
     * Performs a series of operations with the same channel and with confirms.
     * This method will throw an exception if any messaged are n'acked.
     *
     * @param timeout
     * @param closure
     */
    void withConfirmsOrDie(long timeout, @DelegatesTo(RabbitMessagePublisher) Closure closure)

    /**
     * Performs a series of operations with the same channel created from the given connection
     * and with confirms enabled. This method will throw an exception if any messaged are n'acked.
     *
     * @param connection
     * @param timeout
     * @param closure
     */
    void withConfirmsOrDie(String connection, long timeout, @DelegatesTo(RabbitMessagePublisher) Closure closure)
}
