package me.infuzion.web.server.parser.data.node;

import me.infuzion.web.server.parser.Token;
import me.infuzion.web.server.parser.data.node.Node;

public class UnaryOperator extends Node {
    public final Token token;
    public final Node node;

    public UnaryOperator(Token token, Node node){
        this.token = token;
        this.node = node;
    }
}
