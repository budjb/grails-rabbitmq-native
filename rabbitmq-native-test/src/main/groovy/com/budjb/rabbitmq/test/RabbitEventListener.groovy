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

import com.budjb.rabbitmq.event.RabbitEvent
import org.springframework.context.ApplicationListener

class RabbitEventListener implements ApplicationListener<RabbitEvent> {
    Map<Class<RabbitEvent>, Integer> counter = [:]

    @Override
    void onApplicationEvent(RabbitEvent event) {
        increment(event)
    }

    void increment(RabbitEvent event) {
        Class<RabbitEvent> type = (Class<RabbitEvent>) event.getClass()

        if (!counter.containsKey(type)) {
            counter.put(type, 0)
        }
        counter.put(type, counter.get(type)++)
    }

    int getCount(Class<RabbitEvent> type) {
        if (counter.containsKey(type)) {
            return counter.get(type)
        }
        return 0
    }
}
