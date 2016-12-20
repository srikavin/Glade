package me.infuzion.web.server;

import me.infuzion.web.server.event.PageLoadEvent;
import me.infuzion.web.server.listener.RedirectListener;
import me.infuzion.web.server.listener.StatusListener;
import me.infuzion.web.server.listener.TemplateReplacer;

import java.util.ArrayList;
import java.util.List;

public class EventManager {
    private List<PageLoadListener> eventListeners = new ArrayList<>();
    private List<PageLoadListener> eventListenersMonitor = new ArrayList<>();

    public EventManager() {
        new StatusListener(this);
        new RedirectListener(this);
        new TemplateReplacer(this);
    }

    public void callEvent(PageLoadEvent event) {
        for (PageLoadListener e : eventListeners) {
            e.onPageLoad(event);
        }
        for (PageLoadListener e : eventListenersMonitor) {
            e.onPageLoad(event);
        }
    }

    public void registerListener(PageLoadListener listener) {
        registerListener(listener, false);
    }

    public void registerListener(PageLoadListener listener, boolean monitor) {
        if (monitor) {
            eventListenersMonitor.add(listener);
        } else {
            eventListeners.add(listener);
        }
    }
}
