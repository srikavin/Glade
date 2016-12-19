package me.infuzion.web.server.listener;

import me.infuzion.web.server.PageLoadListener;
import me.infuzion.web.server.EventManager;
import me.infuzion.web.server.event.PageLoadEvent;
import me.infuzion.web.server.util.Utilities;

import java.io.InputStream;

public class RedirectListener implements PageLoadListener {

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
