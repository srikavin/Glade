package me.infuzion.web.server;

public interface Listener {
    Class<? extends Event> getEvent();
}
