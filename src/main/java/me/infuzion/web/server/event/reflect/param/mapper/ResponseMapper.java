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
 * Implementing classes handle annotations that map a value returned from the event handler to side-effects on the
 * event itself or elsewhere.
 *
 * @param <A> The type of annotation to handle
 * @param <E> The root event type this predicate handles
 */
public interface ResponseMapper<A extends Annotation, E extends Event, R> extends ParamAnnotationHandler {
    /**
     * Maps the annotated parameter to side-effects on the event or elsewhere.
     *
     * @param annotation  The annotation attached to a given event handler
     * @param event       The event that that the event handler is handling
     * @param returnValue The value that was returned from the event handler
     */
    void map(A annotation, Method method, E event, R returnValue);

    /**
     * Validates the use of this annotation and annotation handler. If this returns true, there should be no exceptions
     * at runtime related to the configuration of this annotation, parameter, and event.
     *
     * @param annotation The annotation attached to a given event handler.
     * @param returnType The type of the parameter that the annotation is attached to.
     * @param event      The type of event that the event handler handles.
     * @return True if the configuration is valid.
     */

    boolean validate(A annotation, Method method, Class<?> returnType, Class<? extends Event> event);
}
