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

import org.springframework.util.MimeType

/**
 * Describes a class that can convert message bodies.
 */
interface MessageConverter {
    /**
     * Determines whether the converter supports conversion to/from the given class type.
     *
     * @param type Class type to check.
     * @return Whether the converter supports the given class type.
     */
    boolean supports(Class<?> type)

    /**
     * Determines whether the converter supports the given mime type.
     *
     * @param mimeType Mime type to check.
     * @return Whether the converter supports the given mime type.
     */
    boolean supports(MimeType mimeType)
}
