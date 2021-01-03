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

import com.google.common.flogger.FluentLogger;
import me.infuzion.web.server.event.Event;
import me.infuzion.web.server.util.Pair;
import org.apache.commons.lang3.time.StopWatch;
import org.sonatype.inject.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PerformanceMetrics {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    private static final ThreadLocal<Event> currentEvent = new ThreadLocal<>();
    private static final ThreadLocal<Double> lastMetric = ThreadLocal.withInitial(() -> (double) System.currentTimeMillis());
    private static final ThreadLocal<Map<String, Pair<StopWatch, String>>> timers = ThreadLocal.withInitial(HashMap::new);

    public static void setCurrentEvent(Event event) {
        currentEvent.set(event);
        event.setPerformanceMetrics(new ArrayList<>());
        lastMetric.set((double) System.currentTimeMillis());
        timers.remove();
    }

    public static void addTag(String name) {
        currentEvent.get().getPerformanceMetrics().add(new PerformanceMetric(name, null, null));
    }

    public static void recordMetric(PerformanceMetric metric) {
        currentEvent.get().getPerformanceMetrics().add(metric);
    }

    public static void recordMetric(String name) {
        recordMetric(name, null, null);
    }

    public static void recordMetric(String name, @Nullable String description) {
        recordMetric(name, description, null);
    }

    public static void recordMetric(String name, @Nullable Double duration) {
        recordMetric(name, null, duration);
    }

    /**
     * Starts a timer with the given name. It can be stopped using {@link #stopTimer(String)} to record it as a
     * metric. If it isn't stopped, the time will be recorded when timing data is read.
     *
     * @param name The name of the timer
     */
    public static void startTimer(String name) {
        startTimer(name, null);
    }

    /**
     * Starts a timer with the given name. It can be stopped using {@link #stopTimer(String)} to record it as a
     * metric. If it isn't stopped, the time will be recorded when timing data is read.
     *
     * @param name        The name of the timer
     * @param description A description of the metric being timed
     */
    public static void startTimer(String name, @Nullable String description) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        timers.get().put(name, new Pair<>(stopWatch, description));
    }

    /**
     * Stops a timer with the given name.
     *
     * @param name The name of the timer
     */
    public static void stopTimer(String name) {
        if (timers.get().containsKey(name)) {
            StopWatch timer = timers.get().get(name).left;
            if (timer.isStopped()) {
                logger.atWarning().log("Timer %s was attempted to be stopped, but was already stopped!", name);
            } else {
                timer.stop();
            }
        } else {
            logger.atWarning().log("Timer %s was attempted to be stopped, but was never started!", name);
        }
    }

    public static void recordMetric(String name, @Nullable String description, @Nullable Double duration) {
        assert (currentEvent.get() != null);
        assert (currentEvent.get().getPerformanceMetrics() != null);
        if (duration == null) {
            duration = System.currentTimeMillis() - lastMetric.get();
        }
        lastMetric.set((double) System.currentTimeMillis());
        recordMetric(new PerformanceMetric(name, description, duration));
    }

    /**
     * @return A value compatible with the Server-Timing header containing the recorded timing data.
     */
    public static String generateServerTimingHeader() {
        assert (currentEvent.get() != null);
        assert (currentEvent.get().getPerformanceMetrics() != null);
        return Stream.concat(
                timers.get().entrySet().stream()
                        .map((e) -> new PerformanceMetric(e.getKey(), e.getValue().right, (double) e.getValue().left.getTime())),
                currentEvent.get().getPerformanceMetrics().stream()
        ).map(PerformanceMetric::toString).collect(Collectors.joining(", "));
    }
}
