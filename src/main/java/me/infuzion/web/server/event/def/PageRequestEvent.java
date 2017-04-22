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

package me.infuzion.web.server.event.def;

import me.infuzion.web.server.event.Event;
import me.infuzion.web.server.util.HTTPMethod;
import me.infuzion.web.server.util.HttpParameters;
import me.infuzion.web.server.util.Utilities;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class PageRequestEvent extends Event {
    private final String page;
    private final String rawURL;
    private final String requestData;
    private final HttpParameters urlParameters;
    private final HttpParameters bodyParameters;
    private final HTTPMethod method;
    private final Map<String, String> headers;
    private final UUID sessionUuid;
    private final Map<String, Object> session;
    private Map<String, String> additionalHeadersToSend = new HashMap<>();
    private boolean handled = false;
    private String responseData = "";
    private int statusCode;
    private String contentType = "text/html";
    public PageRequestEvent(String page, String requestData, String host, String headers,
                            UUID sessionUuid, Map<String, Object> session, HTTPMethod method)
            throws MalformedURLException, UnsupportedEncodingException {
        this.requestData = requestData;
        this.sessionUuid = sessionUuid;
        this.session = session;
        this.method = method;
        URL url = new URL("http://" + host + page);
        this.page = url.getPath();
        this.rawURL = page;

        urlParameters = new HttpParameters("GET", Utilities.splitQuery(url));
        bodyParameters = new HttpParameters("POST", Utilities.parseQuery(requestData));

        Map<String, String> temp = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (headers != null && headers.length() > 0) {
            String[] headerArr = headers.split("\r\n");
            for (String e : headerArr) {
                String[] keyValue = e.split(":", 2);
                if (keyValue.length == 2) {
                    temp.put(keyValue[0].trim(), keyValue[1].trim());
                }
            }
        }
        this.headers = Collections.unmodifiableMap(temp);
    }

    public PageRequestEvent(PageRequestEvent e) {
        this.additionalHeadersToSend = e.additionalHeadersToSend;
        this.page = e.page;
        this.urlParameters = e.urlParameters;
        this.bodyParameters = e.bodyParameters;
        this.rawURL = e.rawURL;
        this.headers = e.headers;
        this.sessionUuid = e.sessionUuid;
        this.session = e.session;
        this.handled = e.handled;
        this.responseData = e.responseData;
        this.contentType = e.contentType;
        this.requestData = e.requestData;
        this.method = e.method;
    }

    public HTTPMethod getMethod() {
        return method;
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

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getRequestData() {
        return requestData;
    }

    public HttpParameters getUrlParameters() {
        return urlParameters;
    }

    public HttpParameters getBodyParameters() {
        return bodyParameters;
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

    public Map<String, Object> getSession() {
        return session;
    }

    public UUID getSessionUuid() {
        return sessionUuid;
    }
}
