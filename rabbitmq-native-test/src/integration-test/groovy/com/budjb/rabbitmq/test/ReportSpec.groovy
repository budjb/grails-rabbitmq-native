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

import com.budjb.rabbitmq.RabbitContext
import grails.test.mixin.integration.Integration
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import spock.lang.Specification

@Integration
class ReportSpec extends Specification {
    RabbitContext rabbitContext

    def 'Verify that the status report contains the expected information'() {
        setup:
        def expected = [
            [
                consumers   : [
                    [
                        fullName     : 'com.budjb.rabbitmq.test.AllTopicConsumer',
                        load         : 0,
                        name         : 'AllTopicConsumer',
                        numConfigured: 1,
                        numConsuming : 1,
                        numProcessing: 0,
                        queue        : 'topic-queue-all',
                        runningState : 'RUNNING'
                    ],
                    [
                            fullName     : 'com.budjb.rabbitmq.test.plugin.ExchangeBindingTopicConsumer',
                            load         : 0,
                            name         : 'ExchangeBindingTopicConsumer',
                            numConfigured: 1,
                            numConsuming : 1,
                            numProcessing: 0,
                            queue        : 'topic-queue-sub-exchange-set',
                            runningState : 'RUNNING'
                    ],
                    [
                        fullName     : 'com.budjb.rabbitmq.test.MessageContextConsumer',
                        load         : 0,
                        name         : 'MessageContextConsumer',
                        numConfigured: 1,
                        numConsuming : 1,
                        numProcessing: 0,
                        queue        : 'message-context',
                        runningState : 'RUNNING'
                    ],
                    [
                        fullName     : 'com.budjb.rabbitmq.test.ReportingConsumer',
                        load         : 0,
                        name         : 'ReportingConsumer',
                        numConfigured: 1,
                        numConsuming : 1,
                        numProcessing: 0,
                        queue        : 'reporting',
                        runningState : 'RUNNING'
                    ],
                    [
                        fullName     : 'com.budjb.rabbitmq.test.SleepingConsumer',
                        load         : 0,
                        name         : 'SleepingConsumer',
                        numConfigured: 1,
                        numConsuming : 1,
                        numProcessing: 0,
                        queue        : 'sleeping',
                        runningState : 'RUNNING'
                    ],
                    [
                        fullName     : 'com.budjb.rabbitmq.test.SpecificTopicConsumer',
                        load         : 0,
                        name         : 'SpecificTopicConsumer',
                        numConfigured: 1,
                        numConsuming : 1,
                        numProcessing: 0,
                        queue        : 'topic-queue-specific',
                        runningState : 'RUNNING'
                    ],
                    [
                        fullName     : 'com.budjb.rabbitmq.test.StringConsumer',
                        load         : 0,
                        name         : 'StringConsumer',
                        numConfigured: 1,
                        numConsuming : 1,
                        numProcessing: 0,
                        queue        : 'string-test',
                        runningState : 'RUNNING'
                    ],
                    [
                        fullName     : 'com.budjb.rabbitmq.test.SubsetTopicConsumer',
                        load         : 0,
                        name         : 'SubsetTopicConsumer',
                        numConfigured: 1,
                        numConsuming : 1,
                        numProcessing: 0,
                        queue        : 'topic-queue-subset',
                        runningState : 'RUNNING'
                    ]
                ],
                host        : 'localhost',
                name        : 'connection1',
                port        : 5672,
                runningState: 'RUNNING',
                virtualHost : 'test1.rabbitmq.budjb.com'
            ],
            [
                consumers   : [
                    [
                        fullName     : 'com.budjb.rabbitmq.test.Connection2Consumer',
                        load         : 0,
                        name         : 'Connection2Consumer',
                        numConfigured: 1,
                        numConsuming : 1,
                        numProcessing: 0,
                        queue        : 'connection2-queue',
                        runningState : 'RUNNING'
                    ]
                ],
                host        : 'localhost',
                name        : 'connection2',
                port        : 5672,
                runningState: 'RUNNING',
                virtualHost : 'test2.rabbitmq.budjb.com'
            ]
        ]

        when:
        def report = new JsonSlurper().parseText(new JsonBuilder(rabbitContext.getStatusReport()).toString())
        report.each {
            it.consumers = it.consumers.sort { it.name }
        }

        then:
        assertList(expected, report)
    }

    void assertList(List expected, List actual){
        assert expected
        assert actual
        assert expected.size() == actual.size()
        expected.eachWithIndex{ v, i ->
            if(v instanceof Map) assertMap(v, actual[i])
            else if(v instanceof List) assertList(v,actual[i])
            else assert v == actual[i]
        }
    }

    void assertMap(Map expected, Map actual){
        assert expected
        assert actual
        assert expected.size() == actual.size()
        expected.each {k,v ->
            if(v instanceof Map) assertMap(v, actual.get(k))
                else if(v instanceof List) assertList(v,actual.get(k))
            else assert v == actual.get(k)
        }
    }
}
