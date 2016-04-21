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
package com.budjb.rabbitmq

import com.budjb.rabbitmq.connection.ConnectionConfiguration
import com.budjb.rabbitmq.connection.ConnectionManager
import com.budjb.rabbitmq.consumer.ConsumerManager
import com.budjb.rabbitmq.converter.MessageConverter
import com.budjb.rabbitmq.converter.MessageConverterManager
import com.budjb.rabbitmq.queuebuilder.QueueBuilder
import com.budjb.rabbitmq.report.ConnectionReport
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import org.apache.commons.lang.NullArgumentException
import org.springframework.beans.factory.InitializingBean

class RabbitContextProxy implements RabbitContext, InitializingBean {
    /**
     * Target rabbit context.
     */
    RabbitContext target

    /**
     * Sets the target rabbit context.
     *
     * @param target
     */
    void setTarget(RabbitContext target) {
        if (target == null) {
            throw new NullArgumentException("rabbit context target can not be null")
        }
        if (target instanceof RabbitContextProxy) {
            throw new IllegalArgumentException("rabbit context target can not be a RabbitContextProxy")
        }
        this.target = target
    }

    /**
     * Ensures a target is set after spring initialization is complete.
     */
    void afterPropertiesSet() {
        assert target != null: "RabbitContext proxy target is required but not set"
    }

    @Override
    void load() {
        target.load()
    }

    @Override
    void start() {
        target.start()
    }

    @Override
    void stop() {
        target.stop()
    }

    @Override
    void registerMessageConverter(MessageConverter converter) {
        target.registerMessageConverter(converter)
    }

    @Override
    void registerConsumer(Object consumer) {
        target.registerConsumer(consumer)
    }

    @Override
    void startConsumers() {
        target.startConsumers()
    }

    @Override
    Channel createChannel() {
        return target.createChannel()
    }

    @Override
    Channel createChannel(String connectionName) {
        return target.createChannel(connectionName)
    }

    @Override
    Connection getConnection() {
        return target.getConnection()
    }

    @Override
    Connection getConnection(String name) {
        return target.getConnection(name)
    }

    @Override
    void setMessageConverterManager(MessageConverterManager messageConverterManager) {
        target.setMessageConverterManager(messageConverterManager)
    }

    @Override
    void setConnectionManager(ConnectionManager connectionManager) {
        target.setConnectionManager(connectionManager)
    }

    @Override
    void setConsumerManager(ConsumerManager consumerManager) {
        target.setConsumerManager(consumerManager)
    }

    @Override
    void setQueueBuilder(QueueBuilder queueBuilder) {
        target.setQueueBuilder(queueBuilder)
    }

    @Override
    void reload() {
        target.reload()
    }

    @Override
    void reset() {
        target.reset()
    }

    @Override
    void startConsumers(String connectionName) {
        target.startConsumers(connectionName)
    }

    @Override
    void startConsumer(String name) {
        target.startConsumer(name)
    }

    @Override
    void stopConsumers() {
        target.stopConsumers()
    }

    @Override
    void stopConsumers(String connectionName) {
        target.stopConsumers(connectionName)
    }

    @Override
    void stopConsumer(String name) {
        target.stopConsumer(name)
    }

    @Override
    void startConnections() {
        target.startConnections()
    }

    @Override
    void startConnection(String name) {
        target.startConnection(name)
    }

    @Override
    void stopConnections() {
        target.stopConnections()
    }

    @Override
    void stopConnection(String name) {
        target.stopConnection(name)
    }

    @Override
    void registerConnection(ConnectionConfiguration configuration) {
        target.registerConnection(configuration)
    }

    @Override
    void start(boolean deferConsumers) {
        target.start(deferConsumers)
    }

    @Override
    void createExchangesAndQueues() {
        target.createExchangesAndQueues()
    }

    /**
     * Get the overall running state of consumers and connections.
     *
     * @return
     */
    @Override
    RunningState getRunningState() {
        return target.getRunningState()
    }

    /**
     * Perform a graceful shutdown of consumers and then disconnect.
     *
     * This method blocks until the full shutdown is complete.
     */
    @Override
    void shutdown() {
        target.shutdown()
    }

    /**
     * Generates a report about all connections and consumers.
     *
     * @return
     */
    @Override
    List<ConnectionReport> getStatusReport() {
        return target.getStatusReport()
    }
}
