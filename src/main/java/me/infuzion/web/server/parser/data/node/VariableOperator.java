package me.infuzion.web.server.parser.data.node;

public class VariableOperator extends Node {
    public final String name;
    public final boolean assigning;

    public VariableOperator(String name, boolean assigning) {
        this.name = name;
        this.assigning = assigning;
    }
}
