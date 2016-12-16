package me.infuzion.web.server.listener;

import com.github.amr.mimetypes.MimeTypes;
import me.infuzion.web.server.EventListener;
import me.infuzion.web.server.EventManager;
import me.infuzion.web.server.event.PageLoadEvent;
import me.infuzion.web.server.util.Utilities;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class StatusListener implements EventListener {
    private MimeTypes mimeTypesInstance;

    public StatusListener(EventManager eventManager) {
        mimeTypesInstance = MimeTypes.getInstance();
        eventManager.registerListener(this, true);
    }

    @Override
    public void onPageLoad(PageLoadEvent event) {
        if (event.getStatusCode() == 0) {
            InputStream stream = getClass().getResourceAsStream("/web/" + event.getPage());
            if (stream != null) {
                try {
                    if (!Files.isRegularFile(Paths.get(getClass().getResource("/web/" + event.getPage()).toURI()))) {
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
                event.setFileEncoding(contentType);
                return;
            }
            event.setStatusCode(404);
            handleStatus(event);
        }
    }

    private void handleStatus(PageLoadEvent event) {
        if (event.getStatusCode() != 200 && event.getStatusCode() != 0 && event.getResponseData().length() <= 0) {
            InputStream stream = getClass().getResourceAsStream("/web/error/" + event.getStatusCode() + ".html");
            if (stream != null) {
                event.setResponseData(Utilities.convertStreamToString(stream));
            } else {
                event.setResponseData(String.valueOf(event.getStatusCode()));
            }
        }
    }

    private String getContentTypeFromExtension(String extension) {
        return mimeTypesInstance.getByExtension(extension).getMimeType();
    }

    private String getContentTypeFromFileName(String name) {
        String extension = name.substring(name.lastIndexOf(".") + 1);
        return getContentTypeFromExtension(extension);
    }
}
