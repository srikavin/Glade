package me.infuzion.web.server.event;

import java.util.ArrayList;
import java.util.List;

public class HandlerList {

    private static List<Listener> allListeners = new ArrayList<>();
    private volatile List<Listener> handlers = new ArrayList<>();

    public static List<Listener> getAllListeners() {
        return allListeners;
    }

    public List<Listener> getHandlers() {
        return handlers;
    }

    public void register(Listener listener) {
        handlers.add(listener);
    }

    public void reset() {
        allListeners = new ArrayList<>();
        handlers = new ArrayList<>();
    }
}
