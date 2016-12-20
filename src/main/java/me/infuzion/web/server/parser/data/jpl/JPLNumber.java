package me.infuzion.web.server.parser.data.jpl;

public class JPLNumber implements JPLDataType {
    private double value;

    public JPLNumber(double v) {
        this.value = v;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public JPLBoolean asBoolean() {
        return value % 2 == 0 ? new JPLBoolean(false) : new JPLBoolean(true);
    }

    @Override
    public JPLNumber asNumber() {
        return this;
    }

    @Override
    public JPLString asString() {
        return new JPLString(String.valueOf(value));
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof JPLNumber) {
            if (((JPLNumber) o).value == value) {
                return true;
            }
        }
        return false;
    }
}
