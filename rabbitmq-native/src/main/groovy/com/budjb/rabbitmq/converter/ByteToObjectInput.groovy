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

import com.budjb.rabbitmq.consumer.MessageConvertMethod
import groovy.transform.CompileStatic
import org.springframework.util.MimeType

import java.nio.charset.Charset

/**
 * Represents a request to convert a byte array to some object.
 */
@CompileStatic
class ByteToObjectInput {
    /**
     * UTF-8 character set.
     */
    final static Charset UTF_8 = Charset.forName('UTF-8')

    /**
     * Bytes to convert.
     */
    final byte[] bytes

    /**
     * Mime type of the data.
     */
    final MimeType mimeType

    /**
     * How the message converter should behave.
     */
    final MessageConvertMethod messageConvertMethod

    /**
     * List of classes the converter should limit its conversion to.
     */
    final List<Class<?>> classFilter

    /**
     * Constructor for conversions that do not have specific mime type, filter, or converter behavior requirements.
     *
     * @param bytes
     */
    ByteToObjectInput(byte[] bytes) {
        this(bytes, (MimeType) null)
    }

    /**
     * Constructor for conversions that do not have specific filter or converter behavior requirements.
     *
     * @param bytes
     * @param contentType
     */
    ByteToObjectInput(byte[] bytes, String contentType) {
        this(bytes, contentType ? MimeType.valueOf(contentType) : null)
    }

    /**
     * Constructor for conversions that do not have specific filter or converter behavior requirements.
     *
     * @param bytes
     * @param mimeType
     */
    ByteToObjectInput(byte[] bytes, MimeType mimeType) {
        this(bytes, mimeType, null, null)
    }

    /**
     * Constructor for conversions with specific mime type, filter, and converter behavior requirements.
     *
     * @param bytes
     * @param mimeType
     * @param messageConvertMethod
     * @param classFilter
     */
    ByteToObjectInput(byte[] bytes, String mimeType, MessageConvertMethod messageConvertMethod, List<Class<?>> classFilter) {
        this(bytes, mimeType ? MimeType.valueOf(mimeType) :  null, messageConvertMethod, classFilter)
    }

    /**
     * Constructor for conversions with specific mime type, filter, and converter behavior requirements.
     *
     * @param bytes
     * @param mimeType
     * @param messageConvertMethod
     * @param classFilter
     */
    ByteToObjectInput(byte[] bytes, MimeType mimeType, MessageConvertMethod messageConvertMethod, List<Class<?>> classFilter) {
        this.bytes = bytes
        this.mimeType = mimeType
        this.messageConvertMethod = messageConvertMethod
        this.classFilter = classFilter
    }

    /**
     * Returns the character set of the data.
     *
     * @return
     */
    Charset getCharset() {
        return mimeType?.getCharset() ?: UTF_8
    }
}
