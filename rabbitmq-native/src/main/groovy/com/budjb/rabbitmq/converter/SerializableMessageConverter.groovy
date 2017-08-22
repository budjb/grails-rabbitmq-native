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

/**
 * A message converter that supports conversion of Java serializable objects.
 */
@CompileStatic
class SerializableMessageConverter implements ByteToObjectConverter, ObjectToByteConverter {
    /**
     * Mime type.
     */
    final static MimeType mimeType = MimeType.valueOf('application/java-serialized-object')

    /**
     * {@inheritDoc}
     */
    @Override
    boolean supports(Class<?> type) {
        return Serializable.isAssignableFrom(type)
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
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(input.getBytes())
            ObjectInputStream ois = new ObjectInputStream(byteArrayInputStream)
            return new ByteToObjectResult(ois.readObject())
        }
        catch (Exception ignored) {
            return null
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    ObjectToByteResult convert(ObjectToByteInput input) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)
            objectOutputStream.writeObject((Serializable) input.getObject())

            return new ObjectToByteResult(byteArrayOutputStream.toByteArray(), mimeType)
        }
        catch (Exception ignored) {
            return null
        }
    }
}
