package com.budjb.rabbitmq;

import org.apache.log4j.Logger;
import org.codehaus.groovy.grails.commons.ArtefactHandlerAdapter;

class MessageConverterArtefactHandler extends ArtefactHandlerAdapter {
    /**
     * Our artefact type.
     */
    public static final String TYPE = "MessageConverter";

    /**
     * Logger.
     */
    private static final Logger log = Logger.getLogger(MessageConverterArtefactHandler.class);

    public MessageConverterArtefactHandler() {
        super(TYPE, GrailsMessageConverterClass.class, DefaultGrailsMessageConverterClass.class, null);
    }

    public boolean isArtefactClass(Class clazz) {
        if (clazz == null) {
            return false;
        }

        return isMessageConverter(clazz);
    }

    public static boolean isMessageConverter(Class clazz) {
        if (MessageConverter.class.isAssignableFrom(clazz)) {
            return true;
        }
        return false;
    }
}
