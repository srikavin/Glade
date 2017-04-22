package me.infuzion.web.server.event.def;

import me.infuzion.web.server.event.Event;

import java.util.*;

public abstract class WebSocketEvent extends Event {
    private final UUID sessionUUID;
    private final String payload;
    private final byte payloadOpCode;
    private final boolean finished;
    private final String page;

    private final List<String> toSendToAll;
    private final Map<UUID, String> toSend;

    private byte responseOpCode;

    WebSocketEvent(UUID sessionUUID, String payload, byte payloadOpCode, boolean finished, String page) {
        this.sessionUUID = sessionUUID;
        this.payload = payload;
        this.payloadOpCode = payloadOpCode;
        this.finished = finished;
        this.page = page;

        responseOpCode = 0x1;
        toSendToAll = new ArrayList<>();
        toSend = new HashMap<>();
    }

    public byte getResponseOpCode() {
        return responseOpCode;
    }

    public void setResponseOpCode(byte responseOpCode) {
        this.responseOpCode = responseOpCode;
    }

    public UUID getSessionUUID() {
        return sessionUUID;
    }

    public String getPayload() {
        return payload;
    }

    public byte getPayloadOpCode() {
        return payloadOpCode;
    }

    public boolean isFinished() {
        return finished;
    }

    public void addMessage(UUID sessionUUID, String payload) {
        toSend.put(sessionUUID, payload);
    }

    public void addMessage(String payload) {
        addMessage(getSessionUUID(), payload);
    }

    public void addSendToAll(String payload) {
        toSendToAll.add(payload);
    }

    public List<String> getToSendToAll() {
        return toSendToAll;
    }

    public Map<UUID, String> getToSend() {
        return toSend;
    }

    public String getPage() {
        return page;
    }
}
