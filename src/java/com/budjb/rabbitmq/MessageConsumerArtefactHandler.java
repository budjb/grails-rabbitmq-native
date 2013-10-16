package com.budjb.rabbitmq;

import grails.util.Holders;

import org.apache.log4j.Logger;
import org.codehaus.groovy.grails.commons.ArtefactHandlerAdapter;
import org.codehaus.groovy.grails.commons.GrailsClass;

class MessageConsumerArtefactHandler extends ArtefactHandlerAdapter {
    /**
     * Our artefact type.
     */
    public static final String TYPE = "MessageConsumer";

    /**
     * Class suffix.
     */
    public static final String SUFFIX = "Consumer";

    /**
     * Logger.
     */
    private static final Logger log = Logger.getLogger(MessageConsumerArtefactHandler.class);

    /**
     * Constructor.
     */
    public MessageConsumerArtefactHandler() {
        super(TYPE, GrailsMessageConsumerClass.class, DefaultGrailsMessageConsumerClass.class, SUFFIX);
    }

    /**
     * Determines if a class is an artefact of the type that this handler owns.
     *
     * @param clazz
     * @return
     */
    public boolean isArtefactClass(Class clazz) {
        if (clazz == null) {
            return false;
        }

        return isConsumer(clazz);
    }

    /**
     * Determines if a class is a RabbitMQ consumer.
     *
     * @param clazz
     * @return
     */
    public static boolean isConsumer(Class clazz) {
        return clazz.getName().endsWith(SUFFIX) && RabbitConsumer.isConsumer(clazz);
    }
}
