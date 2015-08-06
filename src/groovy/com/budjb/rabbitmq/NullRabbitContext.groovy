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
package com.budjb.rabbitmq

import com.budjb.rabbitmq.connection.ConnectionConfiguration
import com.budjb.rabbitmq.connection.ConnectionManager
import com.budjb.rabbitmq.consumer.ConsumerManager
import com.budjb.rabbitmq.converter.MessageConverter
import com.budjb.rabbitmq.converter.MessageConverterManager
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection

/**
 * A null object implementation of the RabbitContext.
 *
 * This is created when the plugin is disabled.
 */
class NullRabbitContext implements RabbitContext {
    @Override
    void load() {

    }

    @Override
    void start() {

    }

    @Override
    void stop() {

    }

    @Override
    void registerMessageConverter(MessageConverter converter) {

    }

    @Override
    void registerConsumer(Object consumer) {

    }

    @Override
    void startConsumers() {

    }

    @Override
    Channel createChannel() {
        throw new UnsupportedOperationException('unable to create a new channel with a disabled rabbit context')
    }

    @Override
    Channel createChannel(String connectionName) {
        throw new UnsupportedOperationException('unable to create a new channel with a disabled rabbit context')
    }

    @Override
    Connection getConnection() {
        throw new UnsupportedOperationException('no connections are available with a disabled rabbit context')
    }

    @Override
    Connection getConnection(String name) {
        throw new UnsupportedOperationException('no connections are available with a disabled rabbit context')
    }

    @Override
    void setMessageConverterManager(MessageConverterManager messageConverterManager) {

    }

    @Override
    void setConnectionManager(ConnectionManager connectionManager) {

    }

    @Override
    void setConsumerManager(ConsumerManager consumerManager) {

    }

    @Override
    void setQueueBuilder(QueueBuilder queueBuilder) {

    }

    @Override
    void reload() {

    }

    @Override
    void reset() {

    }

    @Override
    void startConsumers(String connectionName) {

    }

    @Override
    void startConsumer(String name) {

    }

    @Override
    void stopConsumers() {

    }

    @Override
    void stopConsumers(String connectionName) {

    }

    @Override
    void stopConsumer(String name) {

    }

    @Override
    void startConnections() {

    }

    @Override
    void startConnection(String name) {

    }

    @Override
    void stopConnections() {

    }

    @Override
    void stopConnection(String name) {

    }

    @Override
    void registerConnection(ConnectionConfiguration configuration) {

    }

    @Override
    void start(boolean deferConsumers) {

    }

    @Override
    void createExchangesAndQueues() {

    }

    /**
     * Get the overall running state of consumers and connections.
     *
     * @return
     */
    @Override
    RunningState getRunningState() {
        throw new UnsupportedOperationException('can not get state on a null rabbit context')
    }

    /**
     * Perform a graceful shutdown of consumers and then disconnect.
     *
     * This method blocks until the full shutdown is complete.
     */
    @Override
    void shutdown() {

    }
}
