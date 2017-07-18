package me.infuzion.web.server.router.def;

import me.infuzion.web.server.router.RouteMethod;
import me.infuzion.web.server.util.HTTPMethod;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultRouterTest {
    @Test
    void parseDynamicSegments() {
        DefaultRouter router = new DefaultRouter("/path/a/b/c/", HTTPMethod.GET);
        assertNull(router.parseDynamicSegments("/path/a/b/", RouteMethod.GET));
        assertNull(router.parseDynamicSegments("/path/a/b/c/", RouteMethod.POST, RouteMethod.PATCH, RouteMethod.OPTIONS));
        assertNull(router.parseDynamicSegments("/path/a/", RouteMethod.GET));
        assertNull(router.parseDynamicSegments("/path/a/b/c/d", RouteMethod.GET));
        assertNull(router.parseDynamicSegments("/path/a/b/c", RouteMethod.GET));
        assertNull(router.parseDynamicSegments("/path/", RouteMethod.GET));

        Map<String, String> map = router.parseDynamicSegments("/path/a/b/c/", RouteMethod.GET);
        assertNotNull(map);

        map = router.parseDynamicSegments("/path/a/b/:dyn/", RouteMethod.GET);
        assertNotNull(map);
        assertNotNull(map.get("dyn"));
        assertEquals(map.get("dyn"), "c");


    }

}