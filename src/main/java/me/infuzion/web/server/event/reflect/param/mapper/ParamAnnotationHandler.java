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

package me.infuzion.web.server.event.reflect.param.mapper;

import org.jetbrains.annotations.NotNull;

public interface ParamAnnotationHandler extends Comparable<ParamAnnotationHandler> {
    /**
     * The relative ordering of multiple annotation handlers applied to the same method. Annotation handlers with lower
     * execution orders will be called first.
     *
     * @return The relative position of this annotation handler
     */
    default int executionOrder() {
        return 100;
    }

    @Override
    default int compareTo(@NotNull ParamAnnotationHandler o) {
        return this.executionOrder() - o.executionOrder();
    }
}
