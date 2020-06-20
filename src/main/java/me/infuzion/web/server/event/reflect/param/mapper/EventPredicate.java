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

import me.infuzion.web.server.event.Event;

import java.lang.annotation.Annotation;

/**
 * Implementing classes handle annotations that stop an event from calling a method. This may be useful to perform
 * validation or authentication.
 *
 * @param <A> The type of annotation to handle
 * @param <E> The root event type this predicate handles
 */
public interface EventPredicate<A extends Annotation, E extends Event> extends ParamAnnotationHandler {
    /**
     * Decides whether a given method should be called based on the annotation and the event that the method handles.
     *
     * @param annotation The annotation on the method
     * @param event      The event that originated requests to this mapper
     * @return True if the original method should be called, false otherwise
     */
    boolean shouldCall(A annotation, E event);

    /**
     * Called when shouldCall returns false. This should be used to handle showing errors to the user, etc. in place of
     * calling the original method.
     *
     * @param annotation The annotation on the method
     * @param event      The event that originated requests to this mapper
     */
    default void onCallPrevented(A annotation, E event) {
        // do nothing
    }

    /**
     * Validates the use of this annotation and annotation handler. If this returns true, there should be no exceptions
     * at runtime related to the configuration of this annotation and event.
     *
     * @param annotation The annotation attached to a given event handler.
     * @param event      The type of event that the event handler handles.
     * @return True if the configuration is valid.
     */
    boolean validate(A annotation, Class<? extends Event> event);

    @Override
    default int executionOrder() {
        return 0;
    }
}
