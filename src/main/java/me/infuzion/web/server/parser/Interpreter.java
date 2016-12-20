/*
 *    Copyright 2016 Infuzion
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package me.infuzion.web.server.parser;

import me.infuzion.web.server.parser.data.NodeVisitor;
import me.infuzion.web.server.parser.data.jpl.JPLBoolean;
import me.infuzion.web.server.parser.data.jpl.JPLDataType;
import me.infuzion.web.server.parser.data.jpl.JPLNumber;
import me.infuzion.web.server.parser.data.node.*;
import me.infuzion.web.server.parser.data.node.Number;

import java.util.HashMap;
import java.util.Map;

public class Interpreter implements NodeVisitor {
    private String output = "";
    private Map<String, Variable> variables = new HashMap<>();

    private JPLNumber numberOperation(BinaryOperator node) {
        JPLDataType visitLeft = visit(node.left);
        JPLDataType visitRight = visit(node.right);
        double left = visitLeft.asNumber().getValue();
        double right = visitRight.asNumber().getValue();
        Double toReturn = null;
        if (node.token.getType() == TokenType.OP_PLUS) {
            toReturn = left + right;
        } else if (node.token.getType() == TokenType.OP_MINUS) {
            toReturn = left - right;
        } else if (node.token.getType() == TokenType.OP_MULTIPLY) {
            toReturn = left * right;
        } else if (node.token.getType() == TokenType.OP_DIVIDE) {
            toReturn = left / right;
        } else if (node.token.getType() == TokenType.OP_EXPONENT) {
            toReturn = Math.pow(left, right);
        }
        if (toReturn == null) {
            throw new RuntimeException("Operator: " + node.token.getType().toString() + " not found!");
        }
        return new JPLNumber(toReturn);
    }

    private JPLDataType assignVariable(BinaryOperator node) {
        VariableOperator noOp;
        Node notNoOp;
        if (node.left instanceof VariableOperator) {
            noOp = (VariableOperator) node.left;
            notNoOp = node.right;
        } else if (node.right instanceof VariableOperator) {
            noOp = (VariableOperator) node.right;
            notNoOp = node.left;
        } else {
            throw new RuntimeException("Not assigning to a variable!");
        }
        String name = noOp.name;
        Double d = visit(notNoOp).asNumber().getValue();
        Variable v = new Variable(node.token, name, new JPLNumber(d), new Node());
        variables.put(name, v);
        return new JPLNumber(d);
    }

    @SuppressWarnings("Duplicates")
    @Override
    public JPLDataType visitBinOp(BinaryOperator node) {
        System.out.println("LEFT: " + node.left.getClass().getSimpleName());
        System.out.println("Right: " + node.right.getClass().getSimpleName());
        if (node.token.getType() == TokenType.ASSIGN) {
            return assignVariable(node);
        } else if (node.token.hasBooleanOperator()) {
            return binaryOperation(node);
        } else if (node.token.hasNumericOperator()) {
            return numberOperation(node);
        } else {
            throw new RuntimeException("Unknown Operator: " + node.token.getType());
        }
    }

    @Override
    public JPLDataType visitNum(Number node) {
        return new JPLNumber(node.num);
    }

    @Override
    public JPLDataType visitVarOp(VariableOperator node) {
        if (node.assigning) {
            return new JPLNumber(0);
        } else {
            Variable a = getVariable(node.name);
            if (a != null) {
                return a.value;
            }
        }
        throw new RuntimeException("Variable not initialized!");
    }

    public JPLDataType visitUnOp(UnaryOperator node) {
        Token operator = node.token;
        if (operator.getType() == TokenType.OP_PLUS) {
            return new JPLNumber(+(visit(node.node).asNumber().getValue()));
        } else if (operator.getType() == TokenType.OP_MINUS) {
            return new JPLNumber(-(visit(node.node).asNumber().getValue()));
        } else if (operator.getType() == TokenType.KEYWORD_ECHO) {
            String eval = visit(node.node).asString().toString();
            System.out.println(eval);
            output += eval + "\n";
            return new JPLNumber(0);
        } else if (operator.getType() == TokenType.OP_NOT) {
            return new JPLBoolean(!visit(node.node).asBoolean().getValue());
        }
        throw new RuntimeException("Unknown unary operator!");
    }

    @Override
    public JPLDataType visitNoOp(NoOperator node) {
        if (node.type == NoOpType.Boolean) {
            return node.value;
        }
        throw new RuntimeException("Unknown no operator type:" + node.getClass().getCanonicalName());
    }

    private JPLDataType binaryOperation(BinaryOperator node) {
        switch (node.token.getType()) {
            case OP_NOT_EQUAL:
                return new JPLBoolean(!visit(node.left).equals(visit(node.right)));
            case OP_EQUALS:
                return new JPLBoolean(visit(node.left).equals(visit(node.right)));
            case OP_LT:
                return new JPLBoolean(
                        visit(node.left).asNumber().getValue() < visit(node.right).asNumber().getValue());
            case OP_LTE:
                return new JPLBoolean(
                        visit(node.left).asNumber().getValue() <= visit(node.right).asNumber().getValue());
            case OP_GT:
                return new JPLBoolean(
                        visit(node.left).asNumber().getValue() > visit(node.right).asNumber().getValue());
            case OP_GTE:
                return new JPLBoolean(
                        visit(node.left).asNumber().getValue() >= visit(node.right).asNumber().getValue());

        }
        throw new RuntimeException("Unknown Operator: " + node.token.getType());
    }

    private Variable getVariable(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name);
        }
        return null;
    }

    public String getOutput() {
        return output;
    }

}
