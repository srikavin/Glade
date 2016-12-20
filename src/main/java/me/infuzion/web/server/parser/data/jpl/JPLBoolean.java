package me.infuzion.web.server.parser.data.jpl;

public class JPLBoolean implements JPLDataType {
    private boolean value;

    public JPLBoolean(boolean value) {
        this.value = value;
    }

    public Boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    @Override
    public JPLBoolean asBoolean() {
        return this;
    }

    @Override
    public JPLNumber asNumber() {
        return value ? new JPLNumber(1) : new JPLNumber(0);
    }

    @Override
    public JPLString asString() {
        return new JPLString(String.valueOf(value));
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof JPLBoolean) {
            if (((JPLBoolean) o).value == value) {
                return true;
            }
        }
        return false;
    }
}
