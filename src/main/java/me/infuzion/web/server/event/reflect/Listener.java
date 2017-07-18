package me.infuzion.web.server.event.reflect;

import me.infuzion.web.server.EventListener;
import me.infuzion.web.server.event.Event;
import me.infuzion.web.server.router.EventRoute;

import java.lang.reflect.Method;

public class Listener {

    private final EventPriority priority;
    private final Class<? extends Event> event;
    private final Method listenerMethod;
    private final EventListener eventListener;
    private final EventControl control;
    private final EventRoute route;
    private final boolean dynamicRouting;

    public Listener(EventPriority priority, Class<? extends Event> event, Method listener,
                    EventListener eventListener, EventControl control, EventRoute route, boolean dynamicRouting) {
        this.priority = priority;
        this.event = event;
        this.listenerMethod = listener;
        this.eventListener = eventListener;
        this.control = control;
        this.route = route;
        this.dynamicRouting = dynamicRouting;
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

    public boolean isDynamicRouting() {
        return dynamicRouting;
    }
}