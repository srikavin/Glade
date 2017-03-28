/*
 *    Copyright 2016 Infuzion
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package me.infuzion.web.server.event;

import me.infuzion.web.server.EventListener;
import me.infuzion.web.server.listener.RedirectListener;
import me.infuzion.web.server.listener.StatusListener;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class EventManager {

    private List<Class<? extends Event>> eventTypes = new ArrayList<>();

    public EventManager() {
        /* MUST BE BEFORE REGISTERING ANY LISTENERS */
        registerDefaultEventTypes();
        /* ---------------------------------------- */
        new StatusListener(this);
        new RedirectListener(this);
//        new JPLExecutor(this);
    }

    private void registerDefaultEventTypes() {
        registerEvent(PageRequestEvent.class);
    }

    public void fireEvent(Event event) {
        try {
            fireEvent(event, EventPriority.NORMAL);
            fireEvent(event, EventPriority.MONITOR);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fireEvent(Event event, EventPriority priority) throws Exception {
        for (Listener listener : Event.getAllHandlers()) {
            if (listener.getEvent().equals(event.getClass()) && listener.getPriority().equals(priority)) {
                listener.getListenerMethod().invoke(listener.getEventListener(), event);
            }
        }
    }

    public void registerListener(EventListener listener) {
        Method[] methods = listener.getClass().getDeclaredMethods();
        Method[] eventConditions = new Method[methods.length];
        int i = 0;
        for (Method method : methods) {
            Annotation annotation = method.getAnnotation(EventCondition.class);
            if (annotation == null) {
                continue;
            }
            if (method.getParameterCount() != 1) {
                continue;
            }
            method.setAccessible(true);
            Class parameterType = method.getParameterTypes()[0];

            if (String.class.equals(parameterType)) {
                eventConditions[i] = method;
                i++;
            }
        }
        for (Method method : methods) {
            Annotation annotation = method.getAnnotation(EventHandler.class);
            if (annotation == null) {
                continue;
            }
            EventPriority priority = ((EventHandler) annotation).priority();
            if (method.getParameterCount() != 1) {
                continue;
            }
            method.setAccessible(true);
            Class listenerMethodClass = method.getParameterTypes()[0];

            if (Event.class.isAssignableFrom(listenerMethodClass)) {
                eventTypes.stream()
                        .filter(eventType -> listenerMethodClass.isAssignableFrom(eventType))
                        .forEach(eventType -> Event.getAllHandlers()
                                .add(new Listener(priority, eventType, method, listener, eventConditions)));
            }
        }
    }

    public void registerEvent(Class<? extends Event> event) {
        if (!eventTypes.contains(event)) {
            eventTypes.add(event);
        }
    }
}
