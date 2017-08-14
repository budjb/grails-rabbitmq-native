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
package com.budjb.rabbitmq.consumer

enum MessageConvertMethod {
    /**
     * Disables message conversion entirely.
     */
    DISABLED,

    /**
     * Only converts the message body when the message's Content-Type matches the converter.
     */
    HEADER,

    /**
     * Always attempt conversion for any handler and converter combination until one succeeds.
     */
    ALWAYS
}
