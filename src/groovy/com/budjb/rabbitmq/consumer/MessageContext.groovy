/*
 * Copyright 2013-2015 Bud Byrd
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
package com.budjb.rabbitmq.consumer

import com.budjb.rabbitmq.connection.ConnectionContext
import com.rabbitmq.client.BasicProperties
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope

class MessageContext {
    /**
     * Channel
     */
    Channel channel

    /**
     * Consumer tag.
     */
    String consumerTag

    /**
     * Message envelope
     */
    Envelope envelope

    /**
     * Message properties
     */
    BasicProperties properties

    /**
     * Raw message body
     */
    byte[] body

    /**
     * Connection context associated with the message.
     */
    ConnectionContext connectionContext
}
