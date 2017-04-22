package me.infuzion.web.server.util;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Cookies implements Iterable<HttpCookie> {
    private final List<HttpCookie> cookies;
    private final List<HttpCookie> modified;

    public Cookies(String cookiesHeader) {
        cookies = HttpCookie.parse(cookiesHeader);
        modified = new ArrayList<>();
        cookies.forEach((cookie) -> cookie.setVersion(1));
    }

    public void addCookie(String name, String value) {
        modified.add(new HttpCookie(name, value));
    }

    public HttpCookie getCookie(String key) {
        return Stream.concat(modified.stream(), cookies.stream())
                .filter(httpCookie -> httpCookie.getName().equals(key)).findFirst().orElse(null);
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
