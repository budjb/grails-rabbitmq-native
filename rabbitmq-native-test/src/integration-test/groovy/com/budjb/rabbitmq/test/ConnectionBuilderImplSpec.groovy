/*
 * Copyright 2013-2017 Bud Byrd
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
package com.budjb.rabbitmq.test

import com.budjb.rabbitmq.connection.ConnectionBuilder
import com.budjb.rabbitmq.connection.ConnectionBuilderImpl
import com.budjb.rabbitmq.connection.ConnectionContext
import grails.test.mixin.integration.Integration
import grails.transaction.*
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.*

@Integration
class ConnectionBuilderImplSpec extends Specification {

    @Autowired
    ConnectionBuilder connectionBuilder

    void 'Validate loadConnections(single)'() {
        setup:
        Closure closure = {
            connection(
                isDefault: true,
                host: 'host',
                virtualHost: 'virtualHost',
                username: 'username',
                password: 'password',
            )
        }

        List<ConnectionContext> connections

        when:
        connections = connectionBuilder.loadConnectionContexts(closure)

        then:
        connections.size() == 1
        connections[0].isDefault
        connections[0].configuration.host == 'host'
        connections[0].configuration.virtualHost == 'virtualHost'
        connections[0].configuration.username == 'username'
        connections[0].configuration.password == 'password'
    }

    void 'Validate loadConnections(multi)'() {
        setup:
        Closure closure = {
            connection(
                isDefault: true,
                host: 'host1',
                virtualHost: 'virtualHost1',
                username: 'username1',
                password: 'password1',
            )

            connection(
                host: 'host2',
                virtualHost: 'virtualHost2',
                username: 'username2',
                password: 'password2',
            )
        }

        List<ConnectionContext> connections

        when:
        connections = connectionBuilder.loadConnectionContexts(closure)

        then:
        connections.size() == 2
        connections[0].isDefault
        connections[0].configuration.host == 'host1'
        connections[0].configuration.virtualHost == 'virtualHost1'
        connections[0].configuration.username == 'username1'
        connections[0].configuration.password == 'password1'

        !connections[1].isDefault
        connections[1].configuration.host == 'host2'
        connections[1].configuration.virtualHost == 'virtualHost2'
        connections[1].configuration.username == 'username2'
        connections[1].configuration.password == 'password2'
    }
}
