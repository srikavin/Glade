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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PerformanceMetricTest {

    @Test
    void testToString() {
        PerformanceMetric metric = new PerformanceMetric("missedCache");
        assertEquals("missedCache", metric.getName());
        assertNull(metric.getDuration());
        assertNull(metric.getDescription());
        assertEquals("missedCache", metric.toString());
    }

    @Test
    void testToString2() {
        PerformanceMetric metric = new PerformanceMetric("cpu", 2.4);
        assertEquals("cpu", metric.getName());
        assertEquals(2.4, metric.getDuration());
        assertNull(metric.getDescription());
        assertEquals("cpu;dur=2.4", metric.toString());
    }

    @Test
    void testToString3() {
        PerformanceMetric metric = new PerformanceMetric("cache", "Cache Read");
        assertEquals("cache", metric.getName());
        assertEquals("Cache Read", metric.getDescription());
        assertEquals("cache;desc=\"Cache Read\"", metric.toString());
    }

    @Test
    void testToString4() {
        PerformanceMetric metric = new PerformanceMetric("cache", "Cache Read", 23.2);
        assertEquals("cache", metric.getName());
        assertEquals(23.2, metric.getDuration());
        assertEquals("Cache Read", metric.getDescription());
        assertEquals("cache;dur=23.2;desc=\"Cache Read\"", metric.toString());
    }
}