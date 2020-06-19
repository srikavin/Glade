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

package me.infuzion.web.server.event.reflect.param.mapper.impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to denote that the annotated parameter should be substituted with the body of the request.
 * <p>
 * The body will be treated as JSON, and will be parsed into a new object of the given type. Note that this data
 * should not be trusted.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface BodyParam {
    /**
     * Indicates that the body should be parsed as a single entity. When used with raw, the raw contents of the body
     * are passed as a parameter.
     */
    String ENTIRE_BODY = "$glade_entire_body";

    /**
     * @return The name of the field to pass the value of.
     */
    String value() default ENTIRE_BODY;

    /**
     * If raw is true, the annotated parameter must be a byte[] or a ByteBuffer. Otherwise, an exception will be thrown
     * during initialization.
     *
     * @return Indicates whether the raw contents of the field should be used.
     */
    boolean raw() default false;
}
