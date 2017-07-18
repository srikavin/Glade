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
import me.infuzion.web.server.event.def.FragmentedWebSocketEvent;
import me.infuzion.web.server.event.def.PageRequestEvent;
import me.infuzion.web.server.event.def.WebSocketEvent;
import me.infuzion.web.server.event.def.WebSocketMessageEvent;
import me.infuzion.web.server.event.reflect.*;
import me.infuzion.web.server.listener.RedirectListener;
import me.infuzion.web.server.listener.StatusListener;
import me.infuzion.web.server.listener.WebSocketListener;
import me.infuzion.web.server.router.EventRoute;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EventManager {

    private List<Class<? extends Event>> eventTypes = new ArrayList<>();

    public EventManager() {
        /* MUST BE BEFORE REGISTERING ANY LISTENERS */
        registerDefaultEventTypes();
        /* ---------------------------------------- */
        new StatusListener(this);
        new RedirectListener(this);
        new WebSocketListener(this);
//        new JPLExecutor(this);
    }

    private void registerDefaultEventTypes() {
        registerEvent(PageRequestEvent.class);
        registerEvent(WebSocketEvent.class);
        registerEvent(FragmentedWebSocketEvent.class);
        registerEvent(WebSocketMessageEvent.class);
    }

    public void fireEvent(Event event) {
        try {
            for (Listener listener : HandlerList.getAllListeners()) {
                if (listener.getEvent().equals(event.getClass()) && listener.getControl() == EventControl.FULL) {
                    if (callListener(event, listener)) {
                        return;
                    }
                }
            }
            fireEvent(event, EventPriority.START);
            fireEvent(event, EventPriority.NORMAL);
            fireEvent(event, EventPriority.MONITOR);
            fireEvent(event, EventPriority.END);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean callListener(Event event, Listener listener) throws Exception {
        Object o = listener.getListenerMethod().invoke(listener.getEventListener(), (Object) event);
        return o instanceof Boolean && (boolean) o;
    }

    private void callListener(Event event, Listener listener, Map<String, String> dynSeg) throws Exception {
        listener.getListenerMethod().invoke(listener.getEventListener(), event, dynSeg);
    }

    private void fireEvent(Event event, EventPriority priority) throws Exception {
        try {
            for (Listener listener : HandlerList.getAllListeners()) {
                if (listener.getEvent().equals(event.getClass()) && listener.getPriority().equals(priority)) {
                    EventRoute route = listener.getRoute();
                    if (route != null && listener.isDynamicRouting()) {
                        Map<String, String> dynSeg = event.getRouter().parseDynamicSegments(route.getPath(), route.getMethods());
                        if (dynSeg != null) {
                            callListener(event, listener, dynSeg);
                        }
                        continue;
                    }
//                    System.out.println("Calling " + listener.getListenerMethod().getDeclaringClass() + " - " + listener.getListenerMethod().getName());
                    callListener(event, listener);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void registerListener(EventListener listener) {
        Method[] methods = listener.getClass().getDeclaredMethods();

        for (Method method : methods) {
            Annotation annotation = method.getAnnotation(EventHandler.class);
            Route route = method.getAnnotation(Route.class);
            if (annotation == null) {
                continue;
            }

            if (method.getParameterCount() != 1 && method.getParameterCount() != 2) {
                System.out.println("Invalid method declared: " + method.getDeclaringClass().getName() + " - " + method.getName());
                continue;
            }

            EventRoute routeObj = null;
            boolean dyanmicRouting = false;
            if (route != null) {
                routeObj = new EventRoute(route.path(), route.methods());
                dyanmicRouting = true;
            }


            EventPriority priority = ((EventHandler) annotation).priority();
            EventControl control = ((EventHandler) annotation).control();

            method.setAccessible(true);
            Class listenerMethodClass = method.getParameterTypes()[0];

            if (method.getParameterCount() == 1) {
                System.out.println(
                        "Method " + method.getDeclaringClass().getName() + " - " + method.getName()
                                + " cannot use dynamic routes." +
                                "Second param needs to be of type Map<String, String>");
                dyanmicRouting = false;
                routeObj = null;
            }

            if (Event.class.isAssignableFrom(listenerMethodClass)) {

                for (Class<? extends Event> eventType : eventTypes) {
                    if (listenerMethodClass.isAssignableFrom(eventType)) {
                        if (dyanmicRouting) {
                            Class dynamicSegments = method.getParameterTypes()[1];
                            if (!dynamicSegments.equals(Map.class)) {
                                routeObj = null;
                                System.out.println(
                                        "Method " + method.getDeclaringClass().getName() + " - " + method.getName()
                                                + " cannot use dynamic routes." +
                                                "Second param needs to be of type Map<String, String>");
                            }
                        }
                        Event.getHandler().addListener(
                                new Listener(priority, eventType, method, listener, control, routeObj, dyanmicRouting));

                    }
                }
            }
        }
    }

    public void registerEvent(Class<? extends Event> event) {
        if (!eventTypes.contains(event)) {
            eventTypes.add(event);
        }
    }
}
