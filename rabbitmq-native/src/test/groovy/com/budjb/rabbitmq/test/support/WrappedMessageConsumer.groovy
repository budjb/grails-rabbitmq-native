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
package com.budjb.rabbitmq.test.support

class WrappedMessageConsumer {
    static rabbitConfig = [queue: 'foobar']

    enum Callback {
        ON_RECEIVE,
        ON_SUCCESS,
        ON_FAILURE,
        ON_COMPLETE
    }

    Callback callback

    def handleMessage(def body) {

    }

    def onReceive(def context) {
        callback = Callback.ON_RECEIVE
    }

    def onSuccess(def context) {
        callback = Callback.ON_SUCCESS
    }

    def onFailure(def context, def throwable) {
        callback = Callback.ON_FAILURE
    }

    def onComplete(def context) {
        callback = Callback.ON_COMPLETE
    }
}
