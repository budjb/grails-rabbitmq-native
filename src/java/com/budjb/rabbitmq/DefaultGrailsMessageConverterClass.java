package com.budjb.rabbitmq;

import org.codehaus.groovy.grails.commons.AbstractInjectableGrailsClass;

public class DefaultGrailsMessageConverterClass extends AbstractInjectableGrailsClass implements GrailsMessageConverterClass {
    public static final String MESSAGECONVERTER = "MessageConverter";

    public DefaultGrailsMessageConverterClass(Class clazz) {
        super(clazz, MESSAGECONVERTER);
    }

    public DefaultGrailsMessageConverterClass(Class clazz, String trailingName) {
        super(clazz, trailingName);
    }
}
