package me.infuzion.web.server.parser.data.node;

import java.util.List;

public class Compound extends Node {
    public final List<String> statements;
    public Compound(List<String> statements){
        this.statements = statements;
    }
}
