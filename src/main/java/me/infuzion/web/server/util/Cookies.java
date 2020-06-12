/*
 * Copyright 2020 Srikavin Ramkumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.infuzion.web.server.util;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Cookies {
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
}
