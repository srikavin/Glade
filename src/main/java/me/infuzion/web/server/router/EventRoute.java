package me.infuzion.web.server.router;

public class EventRoute {
    private final String path;
    private final RouteMethod[] methods;

    public EventRoute(String path, RouteMethod... methods) {
        this.path = path;
        this.methods = methods;
    }

    public RouteMethod[] getMethods() {
        return methods;
    }

    public String getPath() {
        return path;
    }
}
