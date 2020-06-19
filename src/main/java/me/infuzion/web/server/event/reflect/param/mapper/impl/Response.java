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
 * Used to denote that the return value should be set as the return value of this method.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Response {
    String UNALTERED_CONTENT_TYPE = "$glade_unaltered_content_type";

    /**
     * @return Indicates whether the value should be processed by a type adapter (e.g. to convert to JSON).
     */
    boolean raw() default false;

    /**
     * By default, the content type will not be changed from the set value.
     *
     * @return The content type to set on the response.
     */
    String value() default UNALTERED_CONTENT_TYPE;
}
