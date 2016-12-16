package me.infuzion.web.server;

import me.infuzion.web.server.event.PageLoadEvent;

public interface EventListener {
    void onPageLoad(PageLoadEvent event);
}
