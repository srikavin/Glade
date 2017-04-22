package me.infuzion.web.server.response;

import me.infuzion.web.server.Server;
import me.infuzion.web.server.event.Event;
import me.infuzion.web.server.event.def.PageRequestEvent;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.util.Map;

public class DefaultResponseGenerator implements ResponseGenerator {
    @Override
    public void generateResponse(Socket socket, Event event) throws Exception {
        long lastRequestTime = System.currentTimeMillis();
        if (!(event instanceof PageRequestEvent)) {
            return;
        }
        Writer writer = getWriterFromSocket(socket);
        PageRequestEvent requestEvent = (PageRequestEvent) event;
        int status = requestEvent.getStatusCode();
        String contentType = requestEvent.getContentType();
        String responseData = requestEvent.getResponseData();
        Map<String, String> headers = requestEvent.getAdditionalHeadersToSend();

        writer.write("HTTP/2.0" + status + "\r\n");
        writeHeaderLine(writer, "Content-Type", contentType + "; charset=UTF-8");

        writeHeaderLine(writer, "Content-Length",
                (responseData != null ? responseData.getBytes("UTF-8").length : 0));

        writeHeaderLine(writer, "X-Powered-By: Java Web Server v", Server.version);

        writeHeaders(writer, headers);

        writeLastHeaderLine(writer, "X-Request-Time", (System.currentTimeMillis() - lastRequestTime));
        writer.append(responseData);
        writer.flush();
        socket.close();
    }

    protected void writeHeaders(Writer writer, Map<String, String> headers) throws IOException {
        for (Map.Entry<String, String> e : headers.entrySet()) {
            writeHeaderLine(writer, e.getKey(), e.getValue());
        }
    }

    protected Writer getWriterFromSocket(Socket socket) throws IOException {
        return new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    protected Writer writeLastHeaderLine(Writer writer, String key, Object value) throws IOException {
        return writeHeaderLine(writer, key, value).append('\n');
    }

    protected Writer writeHeaderLine(Writer writer, String key, Object value) throws IOException {
        return writer.append(key).append(": ").append(String.valueOf(value)).append("\r\n");
    }
}
