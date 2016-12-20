package me.infuzion.web.server.parser.data.jpl;

import java.util.HashMap;
import java.util.Map;

public class JPLArray implements JPLDataType {
    private Map<String, JPLDataType> value = new HashMap<>();

    public JPLArray(Map<String, JPLDataType> value) {
        this.value = value;
    }

    public Map<String, JPLDataType> getValue() {
        return value;
    }

    public void setValue(Map<String, JPLDataType> value) {
        this.value = value;
    }

    public JPLDataType get(String key) {
        return value.get(key);
    }

    public void set(String key, JPLDataType value) {
        this.value.put(key, value);
    }

    @Override
    public JPLBoolean asBoolean() {
        throw new RuntimeException("Cannot convert an array to a boolean!");
    }

    @Override
    public JPLNumber asNumber() {
        throw new RuntimeException("Cannot convert an array to a number!");
    }

    @Override
    public JPLString asString() {
        String toRet = "";
        for (Map.Entry<String, JPLDataType> e : value.entrySet()) {
            toRet += "\"" + e.getKey() + "\" => \"" + e.getValue() + "\", ";
        }
        return new JPLString("{" + toRet.substring(0, toRet.length() - 1) + "}");
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof JPLArray) {
            if (((JPLArray) o).value.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
