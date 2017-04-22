package me.infuzion.web.server.event.reflect;

import me.infuzion.web.server.EventListener;
import me.infuzion.web.server.event.Event;

import java.lang.reflect.Method;

public class Listener {

    private final EventPriority priority;
    private final Class<? extends Event> event;
    private final Method listenerMethod;
    private final EventListener eventListener;
    private final EventControl control;
    private final Method[] conditionMethod;

    public Listener(EventPriority priority, Class<? extends Event> event, Method listener,
                    EventListener eventListener, EventControl control, Method... conditionMethod) {
        this.priority = priority;
        this.event = event;
        this.listenerMethod = listener;
        this.eventListener = eventListener;
        this.control = control;
        this.conditionMethod = conditionMethod;
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

    public Method[] getConditionMethod() {
        return conditionMethod;
    }

    public EventControl getControl() {
        return control;
    }
}