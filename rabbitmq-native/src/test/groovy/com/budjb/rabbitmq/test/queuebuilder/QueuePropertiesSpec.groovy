package com.budjb.rabbitmq.test.queuebuilder

import com.budjb.rabbitmq.exception.InvalidConfigurationException
import com.budjb.rabbitmq.queuebuilder.QueueProperties
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
class QueuePropertiesSpec extends Specification {

    QueueProperties properties

    void 'Validate validate()'() {
        when:
        properties = new QueueProperties(name: 'foo')
        properties.validate()

        then:
        notThrown Exception

        when:
        properties = new QueueProperties([:])
        properties.validate()

        then:
        thrown InvalidConfigurationException

        when:
        properties = new QueueProperties([name: 'foo', binding: [:]])
        properties.validate()

        then:
        thrown InvalidConfigurationException
    }
}
