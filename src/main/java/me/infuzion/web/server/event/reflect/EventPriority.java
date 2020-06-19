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

public enum EventPriority {
    /**
     * Called before all other event priorities
     */
    START(-1),
    /**
     * Called after {@link #START}; before {@link #END} and {@link #MONITOR}
     */
    NORMAL(100),
    /**
     * Called before {@link #MONITOR}
     */
    END(200),
    /**
     * Called after all other event priorities. Should only be used to monitor results and logging purposes
     */
    MONITOR(1000);

    public int getValue() {
        return priority;
    }

    private final int priority;

    EventPriority(int priority) {
        this.priority = priority;
    }
}
