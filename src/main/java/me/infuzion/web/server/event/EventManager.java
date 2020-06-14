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

package me.infuzion.web.server.event;

import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.StackSize;
import me.infuzion.web.server.EventListener;
import me.infuzion.web.server.event.def.FragmentedWebSocketEvent;
import me.infuzion.web.server.event.def.PageRequestEvent;
import me.infuzion.web.server.event.def.WebSocketEvent;
import me.infuzion.web.server.event.def.WebSocketMessageEvent;
import me.infuzion.web.server.event.reflect.*;
import me.infuzion.web.server.event.reflect.param.DefaultTypeConverter;
import me.infuzion.web.server.event.reflect.param.ParameterGenerator;
import me.infuzion.web.server.router.EventRoute;
import me.infuzion.web.server.router.Router;
import me.infuzion.web.server.router.def.DefaultRouter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EventManager {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    private final List<Listener> listeners = new ArrayList<>();

    private final List<Class<? extends Event>> eventTypes = new ArrayList<>();
    private final List<EventRouterEntry<?>> eventRouters = new ArrayList<>();

    public void fireEvent(Event event) throws Exception {
        logger.atFine().withStackTrace(StackSize.MEDIUM).log("Event %s fired", event);
        for (Listener listener : listeners) {
            if (listener.getEvent().equals(event.getClass()) && listener.getControl() == EventControl.FULL) {
                logger.atFiner().log("Calling %s for %s with FULL control", listener.getEventListener().getClass().getName(), event.getName());
                if (callListener(event, listener)) {
                    return;
                }
            }
        }
        fireEvent(event, EventPriority.START);
        fireEvent(event, EventPriority.NORMAL);
        fireEvent(event, EventPriority.MONITOR);
        fireEvent(event, EventPriority.END);
    }

    public EventManager() {
        /* MUST BE BEFORE REGISTERING ANY LISTENERS */
        registerDefaultEventTypes();
        /* ---------------------------------------- */
//        new JPLExecutor(this);
    }

    private void registerDefaultEventTypes() {
        registerEvent(PageRequestEvent.class);
        registerEvent(WebSocketEvent.class);
        registerEvent(FragmentedWebSocketEvent.class);
        registerEvent(WebSocketMessageEvent.class);
    }

    private boolean callListener(Event event, Listener listener) throws Exception {
        logger.atFiner().log("Calling method %s for %s", listener.getListenerMethod(), event);
        Object o = listener.getListenerMethod().invoke(listener.getEventListener(), event);
        return o instanceof Boolean && (boolean) o;
    }

    private void callRoutableListener(RequestEvent event, Listener listener) throws Exception {
        EventRoute route = listener.getRoute();

        if (!route.getMethods().contains(event.getHttpMethod())) {
            return;
        }

        //noinspection rawtypes
        Router r = new DefaultRouter();
        for (EventRouterEntry<?> eventRouter : eventRouters) {
            if (eventRouter.event.equals(event.getClass())) {
                r = eventRouter.router;
                break;
            }
        }

        for (EventRouterEntry<?> e : eventRouters) {
            if (e.event.equals(event.getClass())) {
                r = e.router;
            }
        }

        //noinspection unchecked
        Map<String, String> dynSeg = r.parseDynamicSegments(route.getPath(), event);

        if (dynSeg == null) {
            return;
        }

        Method m = listener.getListenerMethod();
        ParameterGenerator parameters = listener.getParameters();

        Object[] params = parameters.generateMethodParameters(event, dynSeg);
        logger.atFiner().log("Calling routable method %s for %s", m, event);

        Object ret = m.invoke(listener.getEventListener(), params);

        if (listener.shouldHandleReturnValue()) {
            parameters.handleReturnValue(event, ret);
        }
    }

    private void fireEvent(Event event, EventPriority priority) throws Exception {
        for (Listener listener : listeners) {
            if (listener.getEvent().equals(event.getClass()) && listener.getPriority().equals(priority)) {
                if (event instanceof RequestEvent && listener.getRoute() != null) {
                    callRoutableListener((RequestEvent) event, listener);
                    continue;
                }

                callListener(event, listener);
            }
        }
    }

    public void registerListener(EventListener listener) {
        Method[] methods = listener.getClass().getDeclaredMethods();

        for (Method method : methods) {
            EventHandler annotation = method.getAnnotation(EventHandler.class);
            Route route = method.getAnnotation(Route.class);
            if (annotation == null) {
                continue;
            }

            EventRoute routeObj = null;

            if (route != null) {
                routeObj = new EventRoute(route.value(), route.methods());
            }


            EventPriority priority = annotation.priority();
            EventControl control = annotation.control();

            method.setAccessible(true);
            Class<?> listenerMethodClass = method.getParameterTypes()[0];

            // Only events that extend RequestEvent can be
            if (!RequestEvent.class.isAssignableFrom(listenerMethodClass)) {
                routeObj = null;
            }

            boolean handleReturnValue = false;

            if (!method.getReturnType().equals(Void.TYPE)) {
                handleReturnValue = true;
            }

            // Check if listenerMethodClass extends Event
            if (Event.class.isAssignableFrom(listenerMethodClass)) {
                // Check that the event has been registered
                for (Class<? extends Event> eventType : eventTypes) {
                    if (listenerMethodClass.isAssignableFrom(eventType)) {
                        ParameterGenerator parameters = new ParameterGenerator(new DefaultTypeConverter(), method);

                        logger.atInfo().log("New event listener registered (%s) for %s", method, eventType);

                        listeners.add(
                                new Listener(priority, eventType, method, listener, control, routeObj, parameters, handleReturnValue));

                    }
                }
            }
        }
    }

    public <T extends RequestEvent> void registerEventRouter(Class<T> event, Router<T> router) {
        eventRouters.add(new EventRouterEntry<>(event, router));
    }

    private static class EventRouterEntry<K extends RequestEvent> {
        Class<K> event;
        Router<K> router;

        public EventRouterEntry(Class<K> event, Router<K> router) {
            this.event = event;
            this.router = router;
        }
    }

    public void registerEvent(Class<? extends Event> event) {
        if (!eventTypes.contains(event)) {
            eventTypes.add(event);
        }
    }
}
