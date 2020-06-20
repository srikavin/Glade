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
import java.lang.reflect.Method;

/**
 * Implementing classes handle annotations that map a value in the event to a parameter.
 *
 * @param <A> The type of annotation to handle
 * @param <E> The root event type this predicate handles
 */
public interface ParamMapper<A extends Annotation, E extends Event, P> extends ParamAnnotationHandler {
    /**
     * Maps the annotated parameter to another value using the data in the event.
     *
     * @param annotation    The annotation attached to a given event handler
     * @param method        The method the annotation is attached to
     * @param parameterType The type of the parameter. This should be the type of the return value.
     * @param event         The event that that the event handler is handling
     * @return The value that should be passed to the given parameter
     */
    P map(A annotation, Method method, Class<?> parameterType, E event);

    /**
     * Validates the use of this annotation and annotation handler. If this returns true, there should be no exceptions
     * at runtime related to the configuration of this annotation, parameter, and event.
     *
     * @param annotation    The annotation attached to a given event handler
     * @param method        The method the annotation is attached to
     * @param parameterType The type of the parameter that the annotation is attached to
     * @param event         The type of event that the event handler handles.
     * @return True if the configuration is valid.
     */
    boolean validate(A annotation, Method method, Class<?> parameterType, Class<? extends Event> event);
}
