package me.infuzion.web.server.parser.data.node;

import me.infuzion.web.server.parser.Token;

public class Assign extends Node {
    public final Node left;
    public final Node right;
    public final Token token;

    public Assign(Node left, Node right, Token token) {
        this.left = left;
        this.right = right;
        this.token = token;
    }
}
