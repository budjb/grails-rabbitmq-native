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

import grails.config.Config
import grails.core.GrailsApplication
import grails.test.mixin.integration.Integration
import spock.lang.Specification

/**
 * These tests won't even get close to passing if the application can't start up.
 * They are used to highlight and check the configuration as been resolved as expected,
 * after the application has started.
 *
 * @since 09/12/2016
 */
@Integration
class ConfigurationSpec extends Specification {

    GrailsApplication grailsApplication

    void 'the application starts up in test mode happily and has configuration correctly resolved'(){

        when: 'application has started the configuration can be obtained'
        Config config = grailsApplication.config
        Map rabbitmqConfig = config.rabbitmq

        then: 'the config for rabbitmq exists'
        rabbitmqConfig

        and: 'the lists are of the expected size'
        rabbitmqConfig.connections.size() == 2
        rabbitmqConfig.exchanges.size() == 2

        and:
        // For some reason the plugin.yml file is not properly being merged but overwritten
        // Therefore we test to make sure that the queue we expect is not present
        // Once the merge works this test will fail
        rabbitmqConfig.queues.size() == 10
        rabbitmqConfig.queues.every{it.name != 'topic-queue-sub-exchange-unused'}

        // The YAML processor creates org.springframework.beans.factory.config.YamlProcessor.StrictMapAppenderConstructor
        // which are not converted back to maps
        and: 'they are lists'
        rabbitmqConfig.connections instanceof List
        rabbitmqConfig.exchanges instanceof List
        rabbitmqConfig.queues instanceof List
    }
}
