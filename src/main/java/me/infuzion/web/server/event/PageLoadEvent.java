package me.infuzion.web.server.event;

import me.infuzion.web.server.util.HttpParameters;
import me.infuzion.web.server.util.Utilities;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class PageLoadEvent {
    private final String page;
    private final String rawURL;
    private final String requestData;
    private final HttpParameters getParameters = new HttpParameters("GET");
    private final HttpParameters postParameters = new HttpParameters("POST");
    private Map<String, String> additionalHeaders = new HashMap<>();
    private String responseData = "";
    private int statusCode;
    private String fileEncoding = "text/html";

    public PageLoadEvent(String page, String requestData, String host) throws MalformedURLException, UnsupportedEncodingException {
        this.requestData = requestData;
        URL url = new URL("http://" + host + page);
        this.page = url.getPath();
        this.rawURL = page;
        getParameters.init(Utilities.splitQuery(url));
        postParameters.init(Utilities.parseQuery(requestData));
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

    public Map<String, String> getAdditionalHeaders() {
        return additionalHeaders;
    }

    public void addHeader(String key, String value){
        additionalHeaders.put(key, value);
    }
}
