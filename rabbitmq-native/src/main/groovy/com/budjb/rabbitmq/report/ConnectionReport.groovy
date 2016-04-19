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
package com.budjb.rabbitmq.report

import com.budjb.rabbitmq.RunningState

class ConnectionReport {
    /**
     * Running state of the connection.
     */
    RunningState runningState

    /**
     * Name of the connection.
     */
    String name

    /**
     * Connection's host.
     */
    String host

    /**
     * Connection port.
     */
    int port

    /**
     * Connection's virtual host.
     */
    String virtualHost

    /**
     * List of consumers tied to this connection.
     */
    List<ConsumerReport> consumers
}
