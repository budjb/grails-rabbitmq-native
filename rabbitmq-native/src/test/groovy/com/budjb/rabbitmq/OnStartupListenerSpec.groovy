package com.budjb.rabbitmq

import grails.core.GrailsApplication
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.grails.config.PropertySourcesConfig
import org.springframework.context.ApplicationContext
import spock.lang.Specification
import spock.lang.Unroll

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
class OnStartupListenerSpec extends Specification {
    OnStartupListener onStartupListener

    ApplicationContext applicationContext
    GrailsApplication grailsApplication
    RabbitContext rabbitContext

    def setup() {
        applicationContext = Mock(ApplicationContext)
        grailsApplication = Mock(GrailsApplication)
        rabbitContext = Mock(RabbitContext)

        onStartupListener = new OnStartupListener()
        onStartupListener.applicationContext = applicationContext
        onStartupListener.grailsApplication = grailsApplication
    }

    void 'Validate getRabbitContextBean()'() {
        setup:
        RabbitContext argument

        when:
        argument = onStartupListener.getRabbitContextBean()

        then:
        1 * applicationContext.getBean('rabbitContext', RabbitContext) >> rabbitContext
        argument == rabbitContext
    }

    @Unroll
    void 'Validate isEnabled() == #expect (enabled: #enabled)'() {
        setup:
        PropertySourcesConfig config = [
            rabbitmq: [
                enabled: enabled,
            ],
        ]

        grailsApplication.getConfig() >> config

        expect:
        onStartupListener.isEnabled() == expect

        where:
        enabled || expect
        true    || true
        false   || false

        'true'  || true
        'false' || true
        1       || true
        0       || true
        '1'     || true
        '0'     || true
    }

    @Unroll
    void 'Validate isAutoStart() == #expect (autoStart: #autoStart)'() {
        setup:
        PropertySourcesConfig config = [
            rabbitmq: [
                autoStart: autoStart,
            ],
        ]

        grailsApplication.getConfig() >> config

        expect:
        onStartupListener.isAutoStart() == expect

        where:
        autoStart || expect
        true      || true
        false     || false

        'true'    || true
        'false'   || true
        1         || true
        0         || true
        '1'       || true
        '0'       || true
    }

    void 'Validate onStartup() (enabled: true, autoStart: true)'() {
        setup:
        PropertySourcesConfig config = [
            rabbitmq: [
                enabled: true,
                autoStart: true,
            ],
        ]

        grailsApplication.getConfig() >> config

        when:
        onStartupListener.onStartup([:])

        then:
        1 * applicationContext.getBean('rabbitContext', RabbitContext) >> rabbitContext
        1 * rabbitContext.load()
        1 * rabbitContext.start(false)
    }

    void 'Validate onStartup() (enabled: false)'() {
        setup:
        PropertySourcesConfig config = [
            rabbitmq: [
                enabled: false,
            ],
        ]

        grailsApplication.getConfig() >> config

        when:
        onStartupListener.onStartup([:])

        then:
        0 * applicationContext.getBean('rabbitContext', RabbitContext) >> rabbitContext
        0 * rabbitContext.load()
        0 * rabbitContext.start(_)
    }

    void 'Validate onStartup() (enabled: true, autoStart: false)'() {
        setup:
        PropertySourcesConfig config = [
            rabbitmq: [
                enabled: true,
                autoStart: false,
            ],
        ]

        grailsApplication.getConfig() >> config

        when:
        onStartupListener.onStartup([:])

        then:
        1 * applicationContext.getBean('rabbitContext', RabbitContext) >> rabbitContext
        1 * rabbitContext.load()
        1 * rabbitContext.start(true)
    }
}
