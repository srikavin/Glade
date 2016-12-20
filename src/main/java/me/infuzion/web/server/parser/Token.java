package me.infuzion.web.server.parser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Token {
    private static final Set<TokenType> booleanSet = new HashSet<>(Arrays.asList(TokenType.OP_GT, TokenType.OP_GTE,
            TokenType.OP_LT, TokenType.OP_LTE, TokenType.OP_NOT, TokenType.OP_NOT_EQUAL));
    private static final Set<TokenType> numericSet = new HashSet<>(Arrays.asList(TokenType.OP_PLUS, TokenType.OP_MINUS,
            TokenType.OP_MINUS, TokenType.OP_MULTIPLY, TokenType.OP_EXPONENT));
    private final TokenType type;
    private final String value;
    private final boolean hasBooleanOp;
    private final boolean hasNumericOp;

    public Token(TokenType type, String value) {
        this.type = type;
        this.value = value;
        hasBooleanOp = booleanSet.contains(type);
        hasNumericOp = numericSet.contains(type);
    }

    public TokenType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public boolean hasBooleanOperator() {
        return hasBooleanOp;
    }

    public boolean hasNumericOperator() {
        return hasNumericOp;
    }
}
