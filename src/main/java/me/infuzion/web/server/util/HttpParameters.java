package me.infuzion.web.server.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HttpParameters implements Iterable<Map.Entry<String, List<String>>> {
    private Map<String, List<String>> parameters = new HashMap<>();
    private final String method;
    private boolean initialized = false;

    public HttpParameters(String method) {
        this.method = method;
    }

    public void init(Map<String, List<String>> map) {
        if (initialized) {
            return;
        }

        initialized = true;
        this.parameters = map;
    }

    public Map<String, List<String>> getParameters() {
        return parameters;
    }

    public List<String> get(String name){
        return parameters.get(name);
    }

    @Override
    public Iterator<Map.Entry<String, List<String>>> iterator() {
        return parameters.entrySet().iterator();
    }

    public String getMethod() {
        return method;
    }
}
