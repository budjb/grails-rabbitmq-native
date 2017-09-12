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

import com.budjb.rabbitmq.consumer.MessageContext

class AllTopicConsumer {
    static rabbitConfig = [
        connection: 'connection1',
        queue: 'topic-queue-all'
    ]

    public Map lastMessage

    String handleMessage(String body, MessageContext messageContext) {
        recordLastRequest('String', body, messageContext)

        return body
    }

    Integer handleMessage(Integer body, MessageContext messageContext) {
        recordLastRequest('Integer', body, messageContext)

        return body
    }

    List handleMessage(List body, MessageContext messageContext) {
        recordLastRequest('List', body, messageContext)

        return body
    }

    Map handleMessage(Map body, MessageContext messageContext) {
        recordLastRequest('Map', body, messageContext)

        return body
    }

    Object handleMessage(Object body, MessageContext messageContext) {
        recordLastRequest(body.getClass().toString(), body, messageContext)

        return body
    }

    private void recordLastRequest(String type, Object body, MessageContext messageContext) {
        lastMessage = [
            type: type,
            body: body,
            messageContext: messageContext
        ]
    }
}
