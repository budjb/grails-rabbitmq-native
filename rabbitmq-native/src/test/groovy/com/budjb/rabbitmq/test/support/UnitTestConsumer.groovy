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
package com.budjb.rabbitmq.test.support

import com.budjb.rabbitmq.consumer.GrailsMessageConsumer

class UnitTestConsumer extends GrailsMessageConsumer {
    static rabbitConfig = [
        queue: 'test-queue',
        consumers: 5
    ]

    def handleMessage(def body, def context) {
        return body
    }

    void onReceive(def context) {

    }

    def onSuccess(def context) {

    }

    def onComplete(def context) {

    }

    def onFailure(def context) {

    }
}
