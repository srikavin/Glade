/*
 * Copyright 2020 Srikavin Ramkumar
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

package me.infuzion.web.server.event.reflect;

public enum EventControl {
    NORMAL(0),
    /**
     * If this is set, the event handler must return a boolean that indicates if the event handler took full control
     * over the event. If the handler returns true, then no further event handlers will be called.
     */
    FULL(1);

    private final int value;

    EventControl(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
