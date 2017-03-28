package me.infuzion.web.server.event;

import me.infuzion.web.server.EventListener;

import java.lang.reflect.Method;

public class Listener {

    private final EventPriority priority;
    private final Class<? extends Event> event;
    private final Method listenerMethod;
    private final EventListener eventListener;
    private final Method[] conditionMethod;

    public Listener(EventPriority priority, Class<? extends Event> event, Method listener,
                    EventListener eventListener, Method... conditionMethod) {
        this.priority = priority;
        this.event = event;
        this.listenerMethod = listener;
        this.eventListener = eventListener;
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
}