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
package com.budjb.rabbitmq.queuebuilder

import org.slf4j.Logger
import org.slf4j.LoggerFactory

enum MatchType {
    /**
     * Header bindings can match any one header.
     */
    ANY,

    /**
     * Header bindings should match all headers.
     */
    ALL

    /**
     * Logger.
     */
    private static Logger log = LoggerFactory.getLogger(MatchType)

    /**
     * Returns the enum that matches the given value regardless of character case.
     *
     * If no match is found, <code>null</code> is returned.
     *
     * @param val
     * @return
     */
    static MatchType lookup(String val) {
        if (val == null) {
            return null
        }
        try {
            return valueOf(val.toUpperCase())
        }
        catch (IllegalArgumentException e) {
            log.trace("no MatchType with name '${val}' found", e)
            return null
        }
    }
}
