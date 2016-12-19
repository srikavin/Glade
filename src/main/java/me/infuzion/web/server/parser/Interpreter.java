package me.infuzion.web.server.parser;

import me.infuzion.web.server.parser.data.NodeVisitor;
import me.infuzion.web.server.parser.data.node.*;
import me.infuzion.web.server.parser.data.node.Number;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.HashMap;
import java.util.Map;

public class Interpreter implements NodeVisitor {
    private Map<String, Variable> variables = new HashMap<>();

    @SuppressWarnings("Duplicates")
    @Override
    public String visitBinOp(BinaryOperator node) {
        System.out.println("LEFT: " + node.left.getClass().getSimpleName());
        System.out.println("Right: " + node.right.getClass().getSimpleName());
        if (node.token.getType() == TokenType.ASSIGN) {
            NoOperator noOp;
            Node notNoOp;
            if (node.left instanceof NoOperator) {
                noOp = (NoOperator) node.left;
                notNoOp = node.right;
            } else if (node.right instanceof NoOperator) {
                noOp = (NoOperator) node.right;
                notNoOp = node.left;
            } else {
                throw new RuntimeException("Not assigning to a variable!");
            }
            String name = noOp.name;
            Double d = Double.valueOf(visit(notNoOp));
            Variable v = new Variable<>(node.token, name, d, new Node());
            variables.put(name, v);
            return String.valueOf(d);
        }
        String visitLeft = visit(node.left);
        String visitRight = visit(node.right);
        if (NumberUtils.isCreatable(visitLeft) && NumberUtils.isCreatable(visitRight)) {
            double left = NumberUtils.createDouble(visitLeft);
            double right = NumberUtils.createDouble(visitRight);
            double toReturn = 0;
            if (node.token.getType() == TokenType.OP_PLUS) {
                toReturn = left + right;
            } else if (node.token.getType() == TokenType.OP_MINUS) {
                toReturn = left - right;
            } else if (node.token.getType() == TokenType.OP_MULTIPLY) {
                toReturn = left * right;
            } else if (node.token.getType() == TokenType.OP_DIVIDE) {
                toReturn = left / right;
            } else if (node.token.getType() == TokenType.EXPONENT) {
                toReturn = Math.pow(left, right);
            }
            return String.valueOf(toReturn);
        }
        throw new RuntimeException("Operator: " + node.token.getType().toString() + " not found!");
    }

    @Override
    public String visitNum(Number node) {
        return String.valueOf(node.num);
    }

    @Override
    public String visitNoOp(NoOperator node) {
        if (node.assigning) {
            return "";
        } else {
            Variable a = getVariable(node.name);
            if (a != null) {
                return String.valueOf(a.value);
            }
        }
        throw new RuntimeException("Variable not initialized!");
    }

    public String visitUnOp(UnaryOperator node) {
        Token operator = node.token;
        if (operator.getType() == TokenType.OP_PLUS) {
            return String.valueOf(+Double.valueOf(visit(node.node)));
        } else if (operator.getType() == TokenType.OP_MINUS) {
            return String.valueOf(-Double.valueOf(visit(node.node)));
        }
        throw new RuntimeException("Unknown unary operator!");
    }

    private Variable getVariable(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name);
        }
        return null;
    }

}
