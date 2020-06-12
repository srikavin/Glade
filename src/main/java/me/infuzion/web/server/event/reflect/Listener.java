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

import me.infuzion.web.server.EventListener;
import me.infuzion.web.server.event.Event;
import me.infuzion.web.server.event.reflect.param.ParameterGenerator;
import me.infuzion.web.server.router.EventRoute;

import java.lang.reflect.Method;

public class Listener {

    private final EventPriority priority;
    private final Class<? extends Event> event;
    private final Method listenerMethod;
    private final EventListener eventListener;
    private final EventControl control;
    private final EventRoute route;
    private final ParameterGenerator parameters;
    private final boolean handleReturn;

    public Listener(EventPriority priority, Class<? extends Event> event, Method listenerMethod, EventListener eventListener,
                    EventControl control, EventRoute route, ParameterGenerator parameters, boolean handleReturn) {
        this.priority = priority;
        this.event = event;
        this.listenerMethod = listenerMethod;
        this.eventListener = eventListener;
        this.control = control;
        this.route = route;
        this.parameters = parameters;
        this.handleReturn = handleReturn;
    }

    public EventPriority getPriority() {
        return priority;
    }

    public Method getListenerMethod() {
        return listenerMethod;
    }

    public Class<? extends Event> getEvent() {
        return event;
    }

    public EventListener getEventListener() {
        return eventListener;
    }

    public EventControl getControl() {
        return control;
    }

    public EventRoute getRoute() {
        return route;
    }

    public ParameterGenerator getParameters() {
        return parameters;
    }

    public boolean shouldHandleReturnValue() {
        return handleReturn;
    }
}