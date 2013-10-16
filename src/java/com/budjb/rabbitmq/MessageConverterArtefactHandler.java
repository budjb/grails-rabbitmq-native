package com.budjb.rabbitmq;

import org.apache.log4j.Logger;
import org.codehaus.groovy.grails.commons.ArtefactHandlerAdapter;

class MessageConverterArtefactHandler extends ArtefactHandlerAdapter {
    /**
     * Our artefact type.
     */
    public static final String TYPE = "MessageConverter";

    /**
     * Class suffix.
     */
    public static final String SUFFIX = "Converter";

    /**
     * Logger.
     */
    private static final Logger log = Logger.getLogger(MessageConverterArtefactHandler.class);

    /**
     * Constructor.
     */
    public MessageConverterArtefactHandler() {
        super(TYPE, GrailsMessageConverterClass.class, DefaultGrailsMessageConverterClass.class, SUFFIX);
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

        return isMessageConverter(clazz);
    }

    /**
     * Determines if a class is a message converter.
     *
     * @param clazz
     * @return
     */
    public static boolean isMessageConverter(Class clazz) {
        return clazz.getName().endsWith(SUFFIX) && MessageConverter.class.isAssignableFrom(clazz);
    }
}
