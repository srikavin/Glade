package me.infuzion.web.server.parser;

import me.infuzion.web.server.parser.data.node.Node;

public class VariableValue extends Node {
    public final String name;
    public final Node node;

    public VariableValue(String name, Node node) {
        this.name = name;
        this.node = node;
    }
}
