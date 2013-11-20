/*
 * Copyright 2013 Bud Byrd
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

import com.budjb.rabbitmq.MessageConverter
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

class MapMessageConverter extends MessageConverter<Map> {
    @Override
    public boolean canConvertFrom() {
        return true
    }

    @Override
    public boolean canConvertTo() {
        return true
    }

    @Override
    public Map convertTo(byte[] input) {
        try {
            return new JsonSlurper().parseText(new String(input))
        }
        catch (Exception e) {
            return null
        }
    }

    @Override
    public byte[] convertFrom(Map input) {
        return new JsonBuilder(input).toString().getBytes()
    }

    @Override
    public String getContentType() {
        return 'application/json'
    }

}
