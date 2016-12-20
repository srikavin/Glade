package me.infuzion.web.server.parser.data.jpl;

public interface JPLDataType {
    JPLBoolean asBoolean();

    JPLNumber asNumber();

    JPLString asString();

    boolean equals(Object o);
}
