/*
 * Copyright 2013-2016 Bud Byrd
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

import groovy.transform.CompileStatic
import org.springframework.util.MimeType

import java.nio.charset.Charset

/**
 * Represents a request to convert some object to a byte array.
 */
@CompileStatic
class ObjectToByteInput {
    /**
     * UTF-8 character set.
     */
    final static Charset UTF_8 = Charset.forName('UTF-8')

    /**
     * Object to convert.
     */
    final Object object

    /**
     * Charset to encode.
     */
    final Charset charset

    /**
     * Constructor for conversions that use the default character set.
     *
     * @param object
     */
    ObjectToByteInput(Object object) {
        this(object, UTF_8)
    }

    /**
     * Constructor for conversions that require a specific character set.
     *
     * @param object
     * @param charset
     */
    ObjectToByteInput(Object object, Charset charset) {
        this.object = object
        this.charset = charset
    }

    /**
     * Constructor for conversions that require a specific character set contained in the
     * given content type.
     *
     * @param object
     * @param contentType
     */
    ObjectToByteInput(Object object, String contentType) {
        this(object, contentType ? MimeType.valueOf(contentType) : null)
    }

    /**
     * Constructor for conversions that require a specific character set contained in the
     * given content type.
     *
     * @param object
     * @param contentType
     */
    ObjectToByteInput(Object object, MimeType contentType) {
        this(object, contentType?.getCharset() ?: UTF_8)
    }
}
