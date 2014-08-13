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
package com.budjb.rabbitmq;

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
    public boolean isArtefactClass(@SuppressWarnings("rawtypes") Class clazz) {
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
    public static boolean isMessageConverter(@SuppressWarnings("rawtypes") Class clazz) {
        return clazz.getName().endsWith(SUFFIX) && MessageConverter.class.isAssignableFrom(clazz);
    }
}
