/*
 * Copyright 2013-2017 Bud Byrd
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

import grails.core.GrailsClass

/**
 * Describes a class that manages message converters and acts as the entry point for conversion.
 */
interface MessageConverterManager {
    /**
     * Converts from an <pre>Object</pre> to a <pre>byte[]</pre>.
     *
     * @param input
     * @return
     */
    ObjectToByteResult convert(ObjectToByteInput input)

    /**
     * Converts from a <pre>byte[]</pre> to an <pre>Object</pre>.
     *
     * If the input specifies a class filter, the conversions will be limited to only those classes.
     * In the absence of a class filter, any conversion will be considered valid.
     *
     * @param input
     * @return
     */
    ByteToObjectResult convert(ByteToObjectInput input)

    /**
     * Retrieves a list of all registered message converters.
     *
     * @return
     */
    List<MessageConverter> getMessageConverters()

    /**
     * Retrieves a list of registered byte-to-object converters.
     *
     * @return
     */
    List<ByteToObjectConverter> getByteToObjectConverters()

    /**
     * Retrieves a list of registered object-to-byte converters.
     *
     * @return
     */
    List<ObjectToByteConverter> getObjectToByteConverters()

    /**
     * Registers a new message converter.
     *
     * @param messageConverter
     */
    void register(MessageConverter messageConverter)

    /**
     * Registers a new message converter from its Grails artefact.
     *
     * @param artefact
     */
    void register(GrailsClass artefact)

    /**
     * Load any registered Grails artefacts and instantiates and adds built-in message converters.
     */
    void load()

    /**
     * Removes any registered message converters.
     */
    void reset()
}
