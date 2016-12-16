package me.infuzion.web.server.event;

public class PageLoadEvent {
    private final String page;
    private final String requestData;
    private String responseData = "";
    private int statusCode;
    private String fileEncoding = "text/html";

    public PageLoadEvent(String page, String requestData) {
        this.page = page;
        this.requestData = requestData;
    }

    public String getPage(){
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
}
