package me.infuzion.web.server.parser.data.node;

import me.infuzion.web.server.parser.Token;

public class Variable<T> extends Node {
    public final Token token;
    public final String name;
    public final T value;
    public final Node node;

    public Variable(Token token, String name, T value, Node node) {
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
