/*
 * Copyright 2013-2014 Bud Byrd
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
import com.budjb.rabbitmq.RabbitContext
import org.codehaus.groovy.grails.commons.GrailsApplication

class RabbitmqNativeBootStrap {
    RabbitContext rabbitContext
    GrailsApplication grailsApplication

    def init = { servletContext ->
        def autoStartup = grailsApplication.config.rabbitmq.autoStart
        if (!(autoStartup instanceof Boolean) || autoStartup == true) {
            rabbitContext.startConsumers()
        }
    }
}
