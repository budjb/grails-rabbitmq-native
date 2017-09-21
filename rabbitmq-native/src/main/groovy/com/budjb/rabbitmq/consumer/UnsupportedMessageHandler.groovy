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
package com.budjb.rabbitmq.consumer

interface UnsupportedMessageHandler {
    /**
     * Called when there are no message converters and consumer handlers available to process an incoming message.
     *
     * @param messageContext Contains the various bits of data associated with the incoming message.
     * @return Optionally respond to an RPC request with the return of the method. If null, no response will be sent.
     */
    Object handleUnsupportedMessage(MessageContext messageContext)
}
