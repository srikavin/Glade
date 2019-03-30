package me.infuzion.web.server.util;

import java.net.HttpCookie;
import java.util.*;
import java.util.function.Consumer;

public class Cookies implements Iterable<HttpCookie> {
    private final List<HttpCookie> cookies;
    private final List<HttpCookie> modified;

    public Cookies(String cookiesHeader) {
        if (cookiesHeader != null) {
            cookies = HttpCookie.parse(cookiesHeader);
        } else {
            cookies = new ArrayList<>();
        }
        modified = new ArrayList<>();
        cookies.forEach((cookie) -> cookie.setVersion(1));
    }

    public void addCookie(String name, String value) {
        modified.add(new HttpCookie(name, value));
    }

    public HttpCookie getCookie(String key) {
        Optional<HttpCookie> modifiedCookie = modified.stream().filter(e -> e.getName().equals(key)).findFirst();
        if (modifiedCookie.isPresent()) {
            return modifiedCookie.get();
        }

        Optional<HttpCookie> cookie = cookies.stream().filter(e -> e.getName().equals(key)).findFirst();
        return cookie.orElse(null);

    }

    public List<HttpCookie> getModified() {
        return modified;
    }

    @Override
    public Iterator<HttpCookie> iterator() {
        return cookies.iterator();
    }

    @Override
    public void forEach(Consumer<? super HttpCookie> action) {
        cookies.forEach(action);
    }

    @Override
    public Spliterator<HttpCookie> spliterator() {
        return cookies.spliterator();
    }
}
