package com.budjb.rabbitmq

import grails.config.Config
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification
import spock.lang.Unroll

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
class RabbitLifecycleListenerSpec extends Specification {
    RabbitLifecycleListener rabbitLifecycleListener
    RabbitContext rabbitContext
    Config config

    def setup() {
        rabbitContext = Mock(RabbitContext)
        config = Mock(Config)

        rabbitLifecycleListener = new RabbitLifecycleListener()
        rabbitLifecycleListener.rabbitContext = rabbitContext
        rabbitLifecycleListener.configuration = config
    }

    @Unroll
    void 'Validate isEnabled() == #expect (enabled: #enabled)'() {
        setup:
        config.getProperty('rabbitmq.enabled', Boolean, true) >> enabled

        expect:
        rabbitLifecycleListener.isEnabled() == expect

        where:
        enabled || expect
        true    || true
        false   || false

        'true'  || true
        'false' || true
        1       || true
        0       || false
        '1'     || true
        '0'     || true
    }

    @Unroll
    void 'Validate isAutoStart() == #expect (autoStart: #autoStart)'() {
        setup:
        config.getProperty('rabbitmq.autoStart', Boolean, true) >> autoStart

        expect:
        rabbitLifecycleListener.isAutoStart() == expect

        where:
        autoStart || expect
        true      || true
        false     || false

        'true'    || true
        'false'   || true
        1         || true
        0         || false
        '1'       || true
        '0'       || true
    }

    void 'Validate doWithApplicationContext() (enabled: true, autoStart: true)'() {
        setup:
        config.getProperty('rabbitmq.enabled', Boolean, true) >> true
        config.getProperty('rabbitmq.autoStart', Boolean, true) >> true

        when:
        rabbitLifecycleListener.doWithApplicationContext()

        then:
        1 * rabbitContext.load()
        0 * rabbitContext.start()
    }

    void 'Validate doWithApplicationContext() (enabled: true, autoStart: false)'() {
        setup:
        config.getProperty('rabbitmq.enabled', Boolean, true) >> true
        config.getProperty('rabbitmq.autoStart', Boolean, true) >> false

        when:
        rabbitLifecycleListener.doWithApplicationContext()

        then:
        1 * rabbitContext.load()
        0 * rabbitContext.start()
    }

    void 'Validate doWithApplicationContext() (enabled: false, autoStart: false)'() {
        setup:
        config.getProperty('rabbitmq.enabled', Boolean, true) >> false
        config.getProperty('rabbitmq.autoStart', Boolean, true) >> false

        when:
        rabbitLifecycleListener.doWithApplicationContext()

        then:
        0 * rabbitContext.load()
        0 * rabbitContext.start()
    }

    void 'Validate onStartup() (enabled: true, autoStart: true)'() {
        setup:
        config.getProperty('rabbitmq.enabled', Boolean, true) >> true
        config.getProperty('rabbitmq.autoStart', Boolean, true) >> true

        when:
        rabbitLifecycleListener.onStartup([:])

        then:
        0 * rabbitContext.load()
        1 * rabbitContext.start()
    }

    void 'Validate onStartup() (enabled: false)'() {
        setup:
        config.getProperty('rabbitmq.enabled', Boolean, true) >> false

        when:
        rabbitLifecycleListener.onStartup([:])

        then:
        0 * rabbitContext.load()
        0 * rabbitContext.start()
    }

    void 'Validate onStartup() (enabled: true, autoStart: false)'() {
        setup:
        config.getProperty('rabbitmq.enabled', Boolean, true) >> true
        config.getProperty('rabbitmq.autoStart', Boolean, true) >> false

        when:
        rabbitLifecycleListener.onStartup([:])

        then:
        0 * rabbitContext.load()
        0 * rabbitContext.start()
    }
}
