package com.budjb.rabbitmq;

import org.codehaus.groovy.grails.commons.AbstractInjectableGrailsClass;

class DefaultGrailsMessageConsumerClass extends AbstractInjectableGrailsClass implements GrailsMessageConverterClass {
    public static final String MESSAGECONSUMER = "MessageConsumer";

    public DefaultGrailsMessageConsumerClass(Class clazz) {
        super(clazz, MESSAGECONSUMER);
    }

    public DefaultGrailsMessageConsumerClass(Class clazz, String trailingName) {
        super(clazz, trailingName);
    }
}
