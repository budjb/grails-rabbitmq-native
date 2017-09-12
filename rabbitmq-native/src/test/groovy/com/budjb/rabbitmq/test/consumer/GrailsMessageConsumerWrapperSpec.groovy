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
package com.budjb.rabbitmq.test.consumer

import com.budjb.rabbitmq.consumer.GrailsMessageConsumerWrapper
import com.budjb.rabbitmq.consumer.MessageConsumer
import com.budjb.rabbitmq.consumer.MessageContext
import com.budjb.rabbitmq.converter.MessageConverterManager
import com.budjb.rabbitmq.test.support.WrappedMessageConsumer
import grails.core.GrailsApplication
import org.grails.config.PropertySourcesConfig
import spock.lang.Specification

class GrailsMessageConsumerWrapperSpec extends Specification {
    def 'Validate that the handlers of wrapped consumers are called'() {
        setup:
        GrailsApplication grailsApplication = Mock(GrailsApplication)
        grailsApplication.getConfig() >> new PropertySourcesConfig()

        MessageConverterManager messageConverterManager = Mock(MessageConverterManager)
        messageConverterManager.convertFromBytes(_, _) >> 'foobar'

        WrappedMessageConsumer wrappedMessageConsumer = new WrappedMessageConsumer()

        MessageConsumer messageConsumer = new GrailsMessageConsumerWrapper(wrappedMessageConsumer, grailsApplication, messageConverterManager)
        MessageContext messageContext = Mock(MessageContext)

        when: 'the wrapper\'s onReceive is called'
        messageConsumer.onReceive(messageContext)

        then: 'the wrapped consumer\'s onReceive is called'
        wrappedMessageConsumer.callback == WrappedMessageConsumer.Callback.ON_RECEIVE

        when: 'the wrapper\'s onSuccess is called'
        messageConsumer.onSuccess(messageContext)

        then: 'the wrapped consumer\'s onSuccess is called'
        wrappedMessageConsumer.callback == WrappedMessageConsumer.Callback.ON_SUCCESS

        when: 'the wrapper\'s onFailure is called'
        messageConsumer.onFailure(messageContext, new Exception())

        then: 'the wrapped consumer\'s onFailure is called'
        wrappedMessageConsumer.callback == WrappedMessageConsumer.Callback.ON_FAILURE

        when: 'the wrapper\'s onComplete is called'
        messageConsumer.onComplete(messageContext)

        then: 'the wrapped consumer\'s onComplete is called'
        wrappedMessageConsumer.callback == WrappedMessageConsumer.Callback.ON_COMPLETE
    }
}
