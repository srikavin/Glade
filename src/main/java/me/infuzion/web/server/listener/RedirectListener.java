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

import java.io.InputStream;
import me.infuzion.web.server.EventManager;
import me.infuzion.web.server.PageRequestEvent;
import me.infuzion.web.server.event.PageLoadEvent;
import me.infuzion.web.server.util.Utilities;

public class RedirectListener implements PageRequestEvent {

    public RedirectListener(EventManager eventManager){
        eventManager.registerListener(this);
    }

    @Override
    public void onPageLoad(PageLoadEvent event) {
        if(event.getPage().endsWith("/") && !event.isHandled()){
            if(event.getGetParameters().getParameters().containsKey("noredir")){
                event.setStatusCode(200);
                event.setFileEncoding("text/text");
                InputStream stream = getClass().getResourceAsStream("/web/index.jpl");
                event.setResponseData(Utilities.convertStreamToString(stream));
            } else {
                event.setStatusCode(302);
                event.addHeader("Location", "/index.jpl");
                event.setResponseData("");
            }
        }
    }
}
