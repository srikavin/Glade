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

package me.infuzion.web.server.event;

import me.infuzion.web.server.performance.PerformanceMetric;
import me.infuzion.web.server.response.ResponseGenerator;

import java.util.List;

public interface Event {
    default String getName() {
        return getClass().getName();
    }

    ResponseGenerator getResponseGenerator();

    void setResponseGenerator(ResponseGenerator generator);

    /**
     * The time this event was created in nanoseconds (as given by {@link System#nanoTime()}).
     */
    long getCreationTime();

    List<PerformanceMetric> getPerformanceMetrics();

    void setPerformanceMetrics(List<PerformanceMetric> performanceMetrics);
}
