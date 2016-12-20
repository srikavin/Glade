package me.infuzion.web.server.parser.data.node;

import me.infuzion.web.server.parser.Token;
import me.infuzion.web.server.parser.data.jpl.JPLDataType;

public class NoOperator extends Node {
    public final JPLDataType value;
    public final NoOpType type;
    public final Token token;

    public NoOperator(JPLDataType value, NoOpType type, Token token) {
        this.value = value;
        this.type = type;
        this.token = token;
    }
}
