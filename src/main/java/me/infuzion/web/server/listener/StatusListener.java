/*
 *    Copyright 2016 Infuzion
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package me.infuzion.web.server.listener;

import com.github.amr.mimetypes.MimeType;
import com.github.amr.mimetypes.MimeTypes;
import me.infuzion.web.server.EventListener;
import me.infuzion.web.server.event.EventManager;
import me.infuzion.web.server.event.def.PageRequestEvent;
import me.infuzion.web.server.event.reflect.EventHandler;
import me.infuzion.web.server.event.reflect.EventPriority;
import me.infuzion.web.server.util.Utilities;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class StatusListener implements EventListener {

    private MimeTypes mimeTypesInstance;

    public StatusListener(EventManager eventManager) {
        mimeTypesInstance = MimeTypes.getInstance();
        mimeTypesInstance.register(new MimeType("text/html", "jpl"));
        eventManager.registerListener(this);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPageLoad(PageRequestEvent event) {
        if (event.getStatusCode() != 200 && !event.isHandled()) {
            if (event.getStatusCode() != 0 && !event.isHandled()) {
                handleStatus(event);
                return;
            }
            InputStream stream = getClass().getResourceAsStream("/web/" + event.getPage());
            if (stream != null) {
                try {
                    if (!Files.isRegularFile(
                            Paths.get(getClass().getResource("/web/" + event.getPage()).toURI()))) {
                        event.setResponseData(Utilities.convertStreamToString(getClass().getResourceAsStream("/web/index.html")));
                        event.setStatusCode(200);
                        event.setContentType("html");
                        event.setHandled(true);
                        if (true)
                            return;
                        event.setStatusCode(404);
                        handleStatus(event);
                        return;
                    }
                } catch (URISyntaxException e) {
                    event.setStatusCode(404);
                    handleStatus(event);
                    return;
                }
                event.setStatusCode(200);
                event.setResponseData(Utilities.convertStreamToString(stream));
                String contentType = getContentTypeFromFileName(event.getPage());
                event.setContentType(contentType);
                return;
            }
            event.setResponseData(Utilities.convertStreamToString(getClass().getResourceAsStream("/web/index.html")));
            event.setStatusCode(200);
            event.setContentType("html");
            event.setHandled(true);
        }
    }

    private void handleStatus(PageRequestEvent event) {
        if (event.getStatusCode() != 200 && event.getStatusCode() != 0
                && event.getResponseData().length() <= 0) {
            InputStream stream = getClass()
                    .getResourceAsStream("/web/error/" + event.getStatusCode() + ".html");
            if (stream != null) {
                event.setResponseData(Utilities.convertStreamToString(stream));
            } else {
                event.setResponseData(String.valueOf(event.getStatusCode()));
            }
        }
    }

    private String getContentTypeFromFileName(String name) {
        return name.substring(name.lastIndexOf(".") + 1);
    }
}
