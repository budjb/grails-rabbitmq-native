/*
 * Copyright 2016 Bud Byrd
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
package com.budjb.rabbitmq.converter

import com.budjb.rabbitmq.exception.MessageConvertException
import grails.core.GrailsClass

interface MessageConverterManager {
    /**
     * Attempt to marshall a byte array to some other object type as long
     * as that object type has been provided in the given list of classes.
     *
     * @param source
     * @param availableClasses
     * @return
     */
    Object convertFromBytes(byte[] source, List<Class<?>> availableClasses)

    /**
     * Attempt to marshall a byte array to some other object type.
     *
     * @param source
     * @return
     */
    Object convertFromBytes(byte[] source)

    /**
     * Attempt to marshall a byte array to some other object type with a content type hint.
     *
     * @param source
     * @param availableClasses
     * @param contentType
     * @return
     * @throws MessageConvertException when conversion can not be completed.
     */
    Object convertFromBytes(byte[] source, List<Class<?>> availableClasses, String contentType) throws MessageConvertException

    /**
     * Attempt to marshall an object to a byte array.
     *
     * @param source
     * @return
     * @throws MessageConvertException when conversion can not be completed.
     */
    byte[] convertToBytes(Object source) throws MessageConvertException

    /**
     * Retrieves the list of registered message converters.
     *
     * @return
     */
    List<MessageConverter<?>> getMessageConverters()

    /**
     * Registers a new message converter.
     *
     * @param messageConverter
     */
    void register(MessageConverter<?> messageConverter)

    /**
     * Registers a new message converter from its Grails artefact.
     *
     * @param artefact
     */
    void register(GrailsClass artefact)

    /**
     * Load any message converter artefacts.
     */
    void load()

    /**
     * Removes any registered message converters.
     */
    void reset()
}
