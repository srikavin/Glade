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
import me.infuzion.web.server.router.Router;
import me.infuzion.web.server.router.def.DefaultRouter;
import me.infuzion.web.server.util.HTTPMethod;
import me.infuzion.web.server.util.HttpParameters;
import me.infuzion.web.server.util.Utilities;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PageRequestEvent extends Event {
    private final DefaultRouter router;
    private final String page;
    private final String requestData;
    private final HttpParameters urlParameters;
    private final HttpParameters bodyParameters;
    private final HTTPMethod method;
    private final Map<String, String> headers;
    private final UUID sessionUuid;
    private final Map<String, Object> session;
    private Map<String, String> additionalHeadersToSend = new HashMap<>();
    private Map<String, byte[]> rawMultipartFormData;
    private boolean handled = false;
    private int statusCode;
    private String contentType = "text/html";
    private byte[] bytes;

    public PageRequestEvent(String page, String requestData, String host, Map<String, String> headers,
                            UUID sessionUuid, Map<String, Object> session, HTTPMethod method, byte[] raw)
            throws MalformedURLException, UnsupportedEncodingException {
        this.requestData = requestData;
        this.sessionUuid = sessionUuid;
        this.session = session;
        this.method = method;
        URL url = new URL("http://" + host + page);
        this.page = url.getPath();

        this.headers = headers;

        urlParameters = new HttpParameters("GET", Utilities.splitQuery(url));

        Map<String, byte[]> bodyData = getMultipartFormData(this.headers, method, raw);
        rawMultipartFormData = bodyData != null ?
                Collections.unmodifiableMap(bodyData) : Collections.unmodifiableMap(new HashMap<>());

        Map<String, List<String>> postParams;
        if (bodyData != null) {
            postParams = new HashMap<>();
            for (Map.Entry<String, byte[]> e : bodyData.entrySet()) {
                postParams.put(e.getKey(), Collections.singletonList(new String(e.getValue())));
            }
        } else {
            postParams = Utilities.parseQuery(requestData);
        }

        bodyParameters = new HttpParameters("POST", postParams);
        router = new DefaultRouter(page, method);
    }

    private static Map<String, byte[]> getMultipartFormData(Map<String, String> headers, HTTPMethod method, byte[] byteData) {
        if (!(method == HTTPMethod.POST)) {
            return null;
        }
        if (!headers.get("Content-Type").contains("multipart/form-data;")) {
            return null;
        }
        String boundary = null;
        for (String e : headers.get("Content-Type").split(";")) {
            if (e.trim().startsWith("boundary=")) {
                boundary = "--" + e.trim().substring(9);
            }
        }
        if (boundary == null) {
            return null;
        }

        String reqData = new String(byteData, StandardCharsets.ISO_8859_1);

        Map<String, byte[]> formData = new HashMap<>();
        String[] segments = reqData.split(boundary);
        for (String e : segments) {
            int index = e.indexOf("name=\"");
            if (index != -1) {
                index += 7;
                char[] arr = e.toCharArray();
                int endIndex = -1;
                for (int i = index; i < arr.length; i++) {
                    if (arr[i] == '"') {
                        endIndex = i + 1;
                        break;
                    }
                }

                if (endIndex != -1) {
                    char[] nameBytes = new char[endIndex - index];
                    System.arraycopy(arr, index - 1, nameBytes, 0, nameBytes.length);
                    String name = new String(nameBytes);
                    String[] split = e.substring(0, e.length() - 2).split("\r\n\r\n", 2);
                    formData.putIfAbsent(name, split[1].getBytes(StandardCharsets.ISO_8859_1));
                }
            }
        }

        return formData;
    }

    public byte[] getResponseDataRaw() {
        return bytes;
    }

    public HTTPMethod getMethod() {
        return method;
    }

    public String getPage() {
        return page;
    }

    public String getResponseData() {
        return new String(bytes);
    }

    public void setResponseData(String responseData) {
        bytes = responseData.getBytes();
        this.setHandled(true);
    }

    public void setResponseData(byte[] bytes) {
        this.bytes = bytes;
        this.setHandled(true);
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

    public Map<String, byte[]> getRawMultipartFormData() {
        return rawMultipartFormData;
    }

    @Override
    public Router getRouter() {
        return router;
    }
}
