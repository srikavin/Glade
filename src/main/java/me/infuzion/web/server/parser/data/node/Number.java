package me.infuzion.web.server.parser.data.node;

import me.infuzion.web.server.parser.Token;

public class Number extends Node {
    public final double num;
    public final Token token;

    public Number(double num, Token token) {
        this.num = num;
        this.token = token;
    }
}
