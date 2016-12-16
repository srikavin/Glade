package me.infuzion.web.server;

import me.infuzion.web.server.event.PageLoadEvent;
import me.infuzion.web.server.listener.StatusListener;

import java.util.ArrayList;
import java.util.List;

public class EventManager {
    List<EventListener> eventListeners = new ArrayList<>();
    List<EventListener> eventListenersMonitor = new ArrayList<>();

    public EventManager(){
        new StatusListener(this);
    }

    public void callEvent(PageLoadEvent event){
        for(EventListener e: eventListeners){
            e.onPageLoad(event);
        }
        for(EventListener e: eventListenersMonitor){
            e.onPageLoad(event);
        }
    }

    public void registerListener(EventListener listener){
        eventListeners.add(listener);
    }

    public void registerListener(EventListener listener, boolean monitor){
        if(monitor) {
            eventListenersMonitor.add(listener);
        } else {
            registerListener(listener);
        }
    }
}
