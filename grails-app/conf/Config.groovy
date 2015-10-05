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
grails.doc.images = new File('src/docs/images')

log4j = {
    trace 'com.budjb.rabbitmq'
}

rabbitmq {
    connection = {
        connection(
            host: 'localhost',
            username: 'guest',
            password: 'guest',
            virtualHost: 'test1.rabbitmq.budjb.com',
            isDefault: true,
            name: 'connection1'
        )
        connection(
            host: 'localhost',
            username: 'guest',
            password: 'guest',
            virtualHost: 'test2.rabbitmq.budjb.com',
            name: 'connection2'
        )
    }
    queues = {
        connection('connection1') {
            queue(name: 'reporting', autoDelete: true)
            queue(name: 'sleeping', autoDelete: true)
            exchange(name: 'topic-exchange', type: 'topic', autoDelete: true) {
                queue(name: 'topic-queue-all', autoDelete: true, binding: '#')
                queue(name: 'topic-queue-subset', autoDelete: true, binding: 'com.budjb.#')
                queue(name: 'topic-queue-specific', autoDelete: true, binding: 'com.budjb.rabbitmq')
            }
        }
        connection('connection2') {
            queue(name: 'connection2-queue', autoDelete: true)
        }
    }
}
