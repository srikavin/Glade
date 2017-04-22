package me.infuzion.web.server.event.reflect;

import java.util.ArrayList;
import java.util.List;

public class HandlerList {

    private static List<Listener> allListeners = new ArrayList<>();
    private static HandlerList handlerList;

    public static List<Listener> getAllListeners() {
        return allListeners;
    }

    public static HandlerList getHandlerList() {
        if (handlerList == null) {
            handlerList = new HandlerList();
        }
        return handlerList;
    }

    public void addListener(Listener listener) {
        allListeners.add(listener);
        allListeners.sort(((o1, o2) -> EventPriority.compare(o1.getPriority(), o2.getPriority())));
    }

    public void reset() {
        allListeners = new ArrayList<>();
    }
}
