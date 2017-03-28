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

import me.infuzion.web.server.EventListener;
import me.infuzion.web.server.event.EventHandler;
import me.infuzion.web.server.event.EventManager;
import me.infuzion.web.server.event.EventPriority;
import me.infuzion.web.server.event.PageRequestEvent;

public class RedirectListener implements EventListener {

    public RedirectListener(EventManager eventManager) {
        eventManager.registerListener(this);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPageLoad(PageRequestEvent event) {
        if (event.getPage().endsWith("/") && !event.isHandled()) {
            event.setStatusCode(302);
            event.addHeader("Location", "/index.html");
            event.setResponseData("");
        }
    }
}
