/*
 * Copyright 2017 Bud Byrd
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
package com.budjb.rabbitmq.report

import com.budjb.rabbitmq.RunningState

class ConsumerReport {
    /**
     * Running state of the consumer context.
     */
    RunningState runningState

    /**
     * Number of concurrent threads configured for the consumer.
     */
    int numConfigured

    /**
     * Number of consumers currently processing messages.
     */
    int numProcessing

    /**
     * Number of consumers actively consuming from a queue.
     *
     * This number only reflects the number of channels consuming, some of which may
     * be idle if no message are present to process.
     */
    int numConsuming

    /**
     * Name of the consumer.
     */
    String name

    /**
     * Full name of the consumer, including its package name.
     */
    String fullName

    /**
     * Queue the consumer is consuming from.
     */
    String queue

    /**
     * Percent load.
     */
    float load
}
