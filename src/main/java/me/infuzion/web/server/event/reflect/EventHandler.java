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

package me.infuzion.web.server.event.reflect;

import me.infuzion.web.server.event.AbstractEvent;
import me.infuzion.web.server.event.Event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If using {@link EventControl#FULL}, {@code return true} if you want to retain full control or
 * {@code return false} if you want other listeners to be called.
 * <p>
 * If the default value for {@linkplain #value()} is not overwritten, the first parameter of the event handler
 * must be the event this handler is listening for.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventHandler {
    final class NoEventSelected extends AbstractEvent {
    }

    EventPriority priority() default EventPriority.NORMAL;

    EventControl control() default EventControl.NORMAL;

    Class<? extends Event> value() default NoEventSelected.class;
}
