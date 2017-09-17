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

import com.budjb.rabbitmq.RunningState
import com.budjb.rabbitmq.consumer.ConsumerManager
import com.budjb.rabbitmq.event.ConsumerManagerStartingEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener

class ConsumerManagerCheckListener implements ApplicationListener<ConsumerManagerStartingEvent> {
    @Autowired
    ConsumerManager consumerManager

    /**
     * Allowing null is intention here. Tests need to know whether the check actually occurred.
     */
    Boolean started = null

    /**
     * {@inheritDoc}
     */
    @Override
    void onApplicationEvent(ConsumerManagerStartingEvent event) {
        started = consumerManager.getRunningState() == RunningState.RUNNING
    }
}
