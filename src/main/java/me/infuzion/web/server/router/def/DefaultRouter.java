package me.infuzion.web.server.router.def;

import me.infuzion.web.server.router.RouteMethod;
import me.infuzion.web.server.router.Router;
import me.infuzion.web.server.util.HTTPMethod;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class DefaultRouter implements Router {

    private final String visitedPath;
    private final RouteMethod method;

    public DefaultRouter(String visitedPath, HTTPMethod hMethod) {
        this.visitedPath = visitedPath;
        method = RouteMethod.valueOf(hMethod.name());
    }

    public static void main(String[] args) {
        DefaultRouter r = new DefaultRouter("/user/123/123as", HTTPMethod.GET);
        Map<String, String> s = r.parseDynamicSegments("/user/:user_id/:j123as", RouteMethod.GET);
        System.out.println(s);
    }

    /**
     * @param routePath The path expected for the route
     */
    @Override
    public Map<String, String> parseDynamicSegments(String routePath, RouteMethod... rMethod) {
        // "/user/:user_id"
        // "/user/asdbawe"

        if (!ArrayUtils.contains(rMethod, RouteMethod.ANY) && !ArrayUtils.contains(rMethod, method)) {
            return null;
        }

        String[] routePathSplit = routePath.split("/");
        String[] visitedPathSplit = visitedPath.split("/");

        int routeSlashCount = StringUtils.countMatches(routePath, '/');
        int visitedSlashCount = StringUtils.countMatches(visitedPath, '/');

        if (routeSlashCount != visitedSlashCount) {
            return null;
        }

        if (routePathSplit.length != visitedPathSplit.length) {
            return null;
        }

        if (routePath.equalsIgnoreCase("*")) {
            return new HashMap<>();
        }

        Map<String, String> toRet = new HashMap<>();

        for (int i = 0; i < routePathSplit.length; i++) {
            String curRoute = routePathSplit[i];
            String curVisited = visitedPathSplit[i];

            if (curRoute.startsWith(":")) {
                String curDynamicSegment = curRoute.substring(1);
                toRet.put(curDynamicSegment, curVisited);
            } else if (!curRoute.equalsIgnoreCase(curVisited)) {
                return null;
            }

        }

        return toRet;
    }
}
