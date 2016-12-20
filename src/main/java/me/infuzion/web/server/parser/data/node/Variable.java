package me.infuzion.web.server.parser.data.node;

import me.infuzion.web.server.parser.Token;
import me.infuzion.web.server.parser.data.jpl.JPLDataType;

public class Variable extends Node {
    public final Token token;
    public final String name;
    public final JPLDataType value;
    public final Node node;

    public Variable(Token token, String name, JPLDataType value, Node node) {
        this.token = token;
        this.name = name;
        this.value = value;
        this.node = node;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Variable) {
            if (((Variable) o).name.equals(name)) {
                return true;
            }
        }
        return false;
    }
}
