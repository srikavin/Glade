package me.infuzion.web.server.router;

import java.util.Map;

public interface Router {
    /**
     * Parses the dynamic segments of the route and returns them.
     * Should return null if the visitedPath is not valid for routePath
     *
     * @param routePath The path expected for the route
     *                  e.g. "/user/:user_id"
     * @return A map if visitedPath is valid for routePath or null
     */
    Map<String, String> parseDynamicSegments(String routePath, RouteMethod... method);
}
