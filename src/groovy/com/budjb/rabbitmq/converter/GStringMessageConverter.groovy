/*
 * Copyright 2013-2014 Bud Byrd
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

class GStringMessageConverter extends MessageConverter<GString> {
    @Override
    public boolean canConvertFrom() {
        return true
    }

    @Override
    public boolean canConvertTo() {
        return false
    }

    @Override
    public GString convertTo(byte[] input) {
        throw new IllegalStateException("can not convert to a GString")
    }

    @Override
    public byte[] convertFrom(GString input) {
        return input.toString().getBytes()
    }

    @Override
    public String getContentType() {
        return null
    }
}
