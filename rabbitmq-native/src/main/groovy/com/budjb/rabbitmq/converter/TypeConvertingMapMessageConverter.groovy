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

import grails.util.TypeConvertingMap
import groovy.json.JsonException
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order

/**
 * A converter that supports the conversion of a {@link TypeConvertingMap} to and from JSON.
 */
@CompileStatic
@Order(Ordered.HIGHEST_PRECEDENCE)
class TypeConvertingMapMessageConverter extends JsonMessageConverter {
    /**
     * {@inheritDoc}
     */
    @Override
    boolean supports(Class<?> type) {
        return TypeConvertingMap.isAssignableFrom(type)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    ByteToObjectResult convert(ByteToObjectInput input) {
        try {
            def parsed = new JsonSlurper().parse(input.getBytes(), input.getCharset().toString())

            if (parsed instanceof Map) {
                return new ByteToObjectResult(new TypeConvertingMap(parsed))
            }
        }
        catch (JsonException ignored) {
            // noop
        }

        return null
    }
}
