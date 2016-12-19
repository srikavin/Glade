package me.infuzion.web.server.parser.data.node;

public class NoOperator extends Node {
    public final String name;
    public final boolean assigning;

    public NoOperator(String name, boolean assigning) {
        this.name = name;
        this.assigning = assigning;
    }
}
