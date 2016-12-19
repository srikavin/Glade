package me.infuzion.web.server.event;

import me.infuzion.web.server.Event;
import me.infuzion.web.server.util.HttpParameters;
import me.infuzion.web.server.util.Utilities;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class PageLoadEvent extends Event {
    private final String page;
    private final String rawURL;
    private final String requestData;
    private final HttpParameters getParameters = new HttpParameters("GET");
    private final HttpParameters postParameters = new HttpParameters("POST");
    private final Map<String, String> headers;
    private Map<String, String> additionalHeadersToSend = new HashMap<>();
    private boolean handled = false;
    private String responseData = "";
    private int statusCode;
    private String fileEncoding = "text/html";

    public PageLoadEvent(String page, String requestData, String host, String headers) throws MalformedURLException, UnsupportedEncodingException {
        this.requestData = requestData;
        URL url = new URL("http://" + host + page);
        this.page = url.getPath();
        this.rawURL = page;
        getParameters.init(Utilities.splitQuery(url));
        postParameters.init(Utilities.parseQuery(requestData));
        Map<String, String> temp = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (headers != null && headers.length() > 0) {
            String[] headerArr = headers.split("\r\n");
            for (String e : headerArr) {
                String[] keyValue = e.split(":", 2);
                if(keyValue.length == 2) {
                    temp.put(keyValue[0].trim(), keyValue[1].trim());
                }
            }
        }
        this.headers = Collections.unmodifiableMap(temp);
    }

    public String getPage() {
        return page;
    }

    public String getResponseData() {
        return responseData;
    }

    public void setResponseData(String responseData) {
        this.responseData = responseData;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getFileEncoding() {
        return fileEncoding;
    }

    public void setFileEncoding(String fileEncoding) {
        this.fileEncoding = fileEncoding;
    }

    public String getRequestData() {
        return requestData;
    }

    public HttpParameters getGetParameters() {
        return getParameters;
    }

    public HttpParameters getPostParameters() {
        return postParameters;
    }

    public String getRawURL() {
        return rawURL;
    }

    public Map<String, String> getAdditionalHeadersToSend() {
        return additionalHeadersToSend;
    }

    public void addHeader(String key, String value) {
        additionalHeadersToSend.put(key, value);
    }

    public boolean isHandled() {
        return handled;
    }

    public void setHandled(boolean handled) {
        this.handled = handled;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
