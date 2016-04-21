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

import java.lang.reflect.ParameterizedType

abstract class MessageConverter<T> {
    /**
     * Returns the class type this converter is responsible for converting.
     *
     * @return
     */
    Class getType() {
        // TODO: may need to do this better...
        return ((ParameterizedType)getClass().getGenericSuperclass()).actualTypeArguments[0]
    }

    /**
     * Returns the content type this handler can convert.
     *
     * @return Content-type, or null if a content-type does not exist.
     */
    abstract String getContentType()

    /**
     * Returns whether the converter can convert the object from its source type to a byte array.
     *
     * @return
     */
    abstract boolean canConvertFrom()

    /**
     * Returns whether the converter can convert the object from a byte array to its proper type.
     * @return
     */
    abstract boolean canConvertTo()

    /**
     * Converts a byte array to the object type this converter is responsible for.
     *
     * @param input
     * @return
     */
    abstract T convertTo(byte[] input)

    /**
     * Converts an object to a byte array.
     *
     * @param input
     * @return
     */
    abstract byte[] convertFrom(T input)
}
