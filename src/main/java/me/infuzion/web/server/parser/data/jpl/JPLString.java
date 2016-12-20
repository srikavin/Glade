package me.infuzion.web.server.parser.data.jpl;

public class JPLString implements JPLDataType {
    private final String value;

    public JPLString(String value) {
        this.value = value;
    }

    @Override
    public JPLBoolean asBoolean() {
        return new JPLBoolean(Boolean.parseBoolean(value));
    }

    @Override
    public JPLNumber asNumber() {
        return new JPLNumber(Double.parseDouble(value));
    }

    @Override
    public JPLString asString() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof JPLString && value.equals(((JPLString) o).value);
    }

    @Override
    public String toString() {
        return value;
    }
}
