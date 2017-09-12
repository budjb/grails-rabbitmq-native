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

import groovy.transform.CompileStatic
import org.springframework.util.ClassUtils
import org.springframework.util.MimeType

/**
 * A converter that supports the conversion of an integer via its string representation.
 */
@CompileStatic
class IntegerMessageConverter implements ByteToObjectConverter, ObjectToByteConverter {
    /**
     * Mime type.
     */
    private static final MimeType mimeType = MimeType.valueOf('text/plain')

    /**
     * {@inheritDoc}
     */
    @Override
    boolean supports(Class<?> type) {
        return ClassUtils.isAssignable(int, type)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean supports(MimeType mimeType) {
        return mimeType.isCompatibleWith(this.mimeType)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    ByteToObjectResult convert(ByteToObjectInput input) {
        try {
            return new ByteToObjectResult(new String(input.getBytes(), input.getCharset()).toInteger())
        }
        catch (NumberFormatException ignored) {
            return null
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    ObjectToByteResult convert(ObjectToByteInput input) {
        return new ObjectToByteResult(input.getObject().toString().getBytes(input.getCharset()), new MimeType(mimeType, input.getCharset()))
    }
}
