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

package me.infuzion.web.server.performance;

import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PerformanceMetric {
    /**
     * Name of the metric being recorded
     */
    @NotNull
    private final String name;

    /**
     * Optional description of the metric being recorded
     */
    @Nullable
    private final String description;

    /**
     * Optional duration of the metric being recorded
     */
    private final @Nullable Double duration;

    public PerformanceMetric(@NotNull String name) {
        this(name, null, null);
    }

    public PerformanceMetric(@NotNull String name, @Nullable Double duration) {
        this(name, null, duration);
    }

    public PerformanceMetric(@NotNull String name, String description) {
        this(name, description, null);
    }

    public PerformanceMetric(@NotNull String name, @Nullable String description, @Nullable Double duration) {
        this.name = name;
        this.description = description;
        this.duration = duration;
    }

    /**
     * @return A string representation of this metric following compatible with the Server-Timing header
     */
    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder(name);
        if (duration != null) {
            ret.append(";dur=").append(duration);
        }
        if (description != null) {
            ret.append(";desc=\"").append(StringEscapeUtils.escapeJava(description)).append("\"");
        }
        return ret.toString();
    }

    public @NotNull String getName() {
        return name;
    }

    public @Nullable String getDescription() {
        return description;
    }

    public @Nullable Double getDuration() {
        return duration;
    }
}
