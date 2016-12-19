package me.infuzion.web.server;

import me.infuzion.web.server.event.PageLoadEvent;

public interface PageLoadListener extends Listener {
    void onPageLoad(PageLoadEvent event);

    default Class<? extends Event> getEvent() {
        return PageLoadEvent.class;
    }
}
