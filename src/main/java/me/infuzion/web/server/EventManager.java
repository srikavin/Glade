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

package me.infuzion.web.server;

import me.infuzion.web.server.event.PageLoadEvent;
import me.infuzion.web.server.jpl.JPLExecutor;
import me.infuzion.web.server.listener.RedirectListener;
import me.infuzion.web.server.listener.StatusListener;

import java.util.ArrayList;
import java.util.List;

public class EventManager {
    private List<PageLoadListener> eventListeners = new ArrayList<>();
    private List<PageLoadListener> eventListenersMonitor = new ArrayList<>();

    public EventManager() {
        new StatusListener(this);
        new RedirectListener(this);
        new JPLExecutor(this);
    }

    public void callEvent(PageLoadEvent event) {
        for (PageLoadListener e : eventListeners) {
            e.onPageLoad(event);
        }
        for (PageLoadListener e : eventListenersMonitor) {
            e.onPageLoad(event);
        }
    }

    public void registerListener(PageLoadListener listener) {
        registerListener(listener, false);
    }

    public void registerListener(PageLoadListener listener, boolean monitor) {
        if (monitor) {
            eventListenersMonitor.add(listener);
        } else {
            eventListeners.add(listener);
        }
    }
}
