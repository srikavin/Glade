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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Token {
    private static final Set<TokenType> booleanSet = new HashSet<>(Arrays.asList(TokenType.OP_GT, TokenType.OP_GTE,
            TokenType.OP_LT, TokenType.OP_LTE, TokenType.OP_NOT, TokenType.OP_EQUALS, TokenType.OP_NOT_EQUAL));
    private static final Set<TokenType> numericSet = new HashSet<>(Arrays.asList(TokenType.OP_PLUS, TokenType.OP_MINUS,
            TokenType.OP_MINUS, TokenType.OP_MULTIPLY, TokenType.OP_DIVIDE, TokenType.OP_EXPONENT));
    public final int row;
    public final int column;
    private final TokenType type;
    private final String value;
    private final boolean hasBooleanOp;
    private final boolean hasNumericOp;

    public Token(TokenType type, String value, int row, int column) {
        this.type = type;
        this.value = value;
        hasBooleanOp = booleanSet.contains(type);
        hasNumericOp = numericSet.contains(type);
        this.row = row;
        this.column = column;
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
