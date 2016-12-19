package me.infuzion.web.server.parser.data.node;

import me.infuzion.web.server.parser.Token;

public class BinaryOperator extends Node {

    public final Node left;
    public final Node right;
    public final Token token;


    public BinaryOperator(Node left, Token token, Node right){
        this.left = left;
        this.right = right;
        this.token = token;
    }
}
