package me.infuzion.web.server.parser.data;

import me.infuzion.web.server.parser.Parser;
import me.infuzion.web.server.parser.data.node.*;
import me.infuzion.web.server.parser.data.node.Number;

public interface NodeVisitor {
    String visitBinOp(BinaryOperator node);

    String visitNum(Number node);

    String visitNoOp(NoOperator node);

    default String visit(Node node) {
        if (node instanceof BinaryOperator) {
            return visitBinOp((BinaryOperator) node);
        } else if (node instanceof Number) {
            return visitNum((Number) node);
        } else if (node instanceof UnaryOperator) {
            return visitUnOp((UnaryOperator) node);
        } else if (node instanceof NoOperator) {
            return visitNoOp((NoOperator) node);
        }
        throw new RuntimeException("No vistor found!");
    }

    default String interpret(Parser parser) {
        return visit(parser.calc());
    }

    String visitUnOp(UnaryOperator node);

}
