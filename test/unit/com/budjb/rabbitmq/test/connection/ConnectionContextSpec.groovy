/*
 * Copyright 2015 Bud Byrd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.budjb.rabbitmq.test.connection

import com.budjb.rabbitmq.connection.ConnectionConfiguration
import com.budjb.rabbitmq.connection.ConnectionContext
import com.budjb.rabbitmq.consumer.ConsumerAdapter
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

import spock.lang.Specification

class ConnectionContextSpec extends Specification {
    def 'Verify RabbitMQ connection factory parameters'() {
        setup:
        ConnectionConfiguration connectionConfiguration = new ConnectionConfiguration([
            'host': 'localhost',
            'username': 'guest',
            'password': 'guest'
        ])
        ConnectionFactory connectionFactory = Mock(ConnectionFactory)
        ConnectionContext connectionContext = new ConnectionContext(connectionConfiguration)
        connectionContext.setConnectionFactory(connectionFactory)

        when:
        connectionContext.openConnection()

        then:
        1 * connectionFactory.newConnection(_)
        1 * connectionFactory.setHost('localhost')
        1 * connectionFactory.setUsername('guest')
        1 * connectionFactory.setPassword('guest')
    }

    def 'Verify lazy-loading connection behavior'() {
        ConnectionConfiguration connectionConfiguration = new ConnectionConfiguration([
            'host': 'localhost',
            'username': 'guest',
            'password': 'guest',
            'ssl': true,
            'threads': 5
        ])
        ConnectionFactory connectionFactory = Mock(ConnectionFactory)
        ConnectionContext connectionContext = new ConnectionContext(connectionConfiguration)
        connectionContext.setConnectionFactory(connectionFactory)

        when:
        connectionContext.getConnection()

        then:
        1 * connectionFactory.newConnection(_)

        when:
        connectionContext.connection = Mock(Connection)
        connectionContext.getConnection()

        then:
        0 * connectionFactory.newConnection(_)
    }

    def 'Validate startConsumers() functionality'() {
        setup:
        ConsumerAdapter consumer1 = Mock(ConsumerAdapter)
        ConsumerAdapter consumer2 = Mock(ConsumerAdapter)
        ConsumerAdapter consumer3 = Mock(ConsumerAdapter)

        ConnectionConfiguration connectionConfiguration = new ConnectionConfiguration([
            'host': 'localhost',
            'username': 'guest',
            'password': 'guest'
        ])
        Connection connection = Mock(Connection)
        connection.isOpen() >> true
        ConnectionFactory connectionFactory = Mock(ConnectionFactory)
        connectionFactory.newConnection(*_) >> connection
        ConnectionContext connectionContext = new ConnectionContext(connectionConfiguration)
        connectionContext.setConnectionFactory(connectionFactory)
        connectionContext.registerConsumer(consumer1)
        connectionContext.registerConsumer(consumer2)
        connectionContext.registerConsumer(consumer3)

        connectionContext.openConnection()

        when:
        connectionContext.startConsumers()

        then:
        1 * consumer1.start()
        1 * consumer2.start()
        1 * consumer3.start()
    }

    def 'Validate stopConsumers() functionality'() {
        setup:
        ConsumerAdapter consumer1 = Mock(ConsumerAdapter)
        ConsumerAdapter consumer2 = Mock(ConsumerAdapter)
        ConsumerAdapter consumer3 = Mock(ConsumerAdapter)

        ConnectionConfiguration connectionConfiguration = new ConnectionConfiguration([
            'host': 'localhost',
            'username': 'guest',
            'password': 'guest'
        ])
        Connection connection = Mock(Connection)
        connection.isOpen() >> true
        ConnectionFactory connectionFactory = Mock(ConnectionFactory)
        connectionFactory.newConnection(*_) >> connection
        ConnectionContext connectionContext = new ConnectionContext(connectionConfiguration)
        connectionContext.setConnectionFactory(connectionFactory)
        connectionContext.registerConsumer(consumer1)
        connectionContext.registerConsumer(consumer2)
        connectionContext.registerConsumer(consumer3)

        connectionContext.openConnection()

        when:
        connectionContext.stopConsumers()

        then:
        1 * consumer1.stop()
        1 * consumer2.stop()
        1 * consumer3.stop()
    }

    def 'Validate closeConnection() functionality'() {
        setup:
        ConsumerAdapter consumer1 = Mock(ConsumerAdapter)
        ConsumerAdapter consumer2 = Mock(ConsumerAdapter)
        ConsumerAdapter consumer3 = Mock(ConsumerAdapter)

        ConnectionConfiguration connectionConfiguration = new ConnectionConfiguration([
            'host': 'localhost',
            'username': 'guest',
            'password': 'guest'
        ])
        Connection connection = Mock(Connection)
        connection.isOpen() >> true
        ConnectionFactory connectionFactory = Mock(ConnectionFactory)
        connectionFactory.newConnection(*_) >> connection
        ConnectionContext connectionContext = new ConnectionContext(connectionConfiguration)
        connectionContext.setConnectionFactory(connectionFactory)
        connectionContext.registerConsumer(consumer1)
        connectionContext.registerConsumer(consumer2)
        connectionContext.registerConsumer(consumer3)

        connectionContext.openConnection()

        when:
        connectionContext.closeConnection()

        then:
        1 * connection.close()

        when:
        connectionContext.closeConnection()

        then:
        0 * connection.close()
    }

    def 'Validate registerConsumer() behavior'() {
        when:
        ConnectionContext connectionContext = new ConnectionContext(null)

        then:
        connectionContext.adapters.size() == 0

        when:
        connectionContext.registerConsumer(Mock(ConsumerAdapter))

        then:
        connectionContext.adapters.size() == 1
    }
}
