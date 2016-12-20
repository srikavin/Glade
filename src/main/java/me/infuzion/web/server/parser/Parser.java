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

import me.infuzion.web.server.parser.data.jpl.JPLBoolean;
import me.infuzion.web.server.parser.data.node.*;
import me.infuzion.web.server.parser.data.node.Number;
import me.infuzion.web.server.parser.exception.ParseException;

import java.util.Scanner;

@SuppressWarnings("Duplicates")
public class Parser {
    private int row;
    private int column;
    private Token currentToken;
    private JPLLexer lexer;

    public Parser(JPLLexer lexer) {
        this.lexer = lexer;
        currentToken = lexer.getNextToken();
        row = 1;
        column = 1;
    }

    public static void main(String[] a) throws ParseException {
        JPLLexer lexer = new JPLLexer("3+43-43+2");
        Parser parser = new Parser(lexer);
        Interpreter interpreter = new Interpreter();
        System.out.println(interpreter.interpret(parser).asString());

        parser = new Parser(new JPLLexer("3  -  5 +  2"));
        System.out.println(interpreter.interpret(parser).asString());

        parser = new Parser(new JPLLexer("3  *  5 +  2"));
        System.out.println(interpreter.interpret(parser).asString());

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            try {
                lexer = new JPLLexer(scanner.nextLine());
                parser = new Parser(lexer);
                System.out.println(interpreter.interpret(parser).asString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void eat(TokenType type) throws ParseException {
        if (currentToken.getType().equals(type)) {
            currentToken = lexer.getNextToken();
            return;
        }
        throw new ParseException(row, column);
    }

    private Node factor() {
        Token token = currentToken;
        if (token.getType() == TokenType.TYPE_NUMBER) {
            eat(TokenType.TYPE_NUMBER);
            return new Number(Double.valueOf(token.getValue()), token);
        } else if (token.getType() == TokenType.PARENTHESIS_LEFT) {
            eat(TokenType.PARENTHESIS_LEFT);
            Node node = calc();
            eat(TokenType.PARENTHESIS_RIGHT);
            return node;
        } else if (token.getType() == TokenType.OP_PLUS) {
            eat(TokenType.OP_PLUS);
            return new UnaryOperator(token, factor());
        } else if (token.getType() == TokenType.OP_MINUS) {
            eat(TokenType.OP_MINUS);
            return new UnaryOperator(token, factor());
        } else if (token.getType() == TokenType.OP_NOT) {
            eat(TokenType.OP_NOT);
            return new UnaryOperator(token, factor());
        } else if (currentToken.getType() == TokenType.KEYWORD_VAR) {
            eat(TokenType.KEYWORD_VAR);
            if (currentToken.getType() == TokenType.VAR_NAME) {
                String name = currentToken.getValue();
                eat(TokenType.VAR_NAME);
                if (currentToken.getType() == TokenType.SEMI) {
                    token = currentToken;
                    eat(TokenType.SEMI);
                    return new Variable(token, name, null, factor());
                } else if (currentToken.getType() == TokenType.ASSIGN) {
                    token = currentToken;
                    eat(TokenType.ASSIGN);
                    if (currentToken.getType() == TokenType.TYPE_NUMBER || currentToken.getType() == TokenType.VAR_NAME) {
                        return new BinaryOperator(new VariableOperator(name, true), token, calc());
                    }
                }
            }
            throw new ParseException(row, column);
        } else if (currentToken.getType() == TokenType.VAR_NAME) {
            String name = currentToken.getValue();
            eat(TokenType.VAR_NAME);
            if (currentToken.getType() == TokenType.ASSIGN) {
                token = currentToken;
                eat(TokenType.ASSIGN);
                if (currentToken.getType() == TokenType.TYPE_NUMBER || currentToken.getType() == TokenType.VAR_NAME) {
                    return new BinaryOperator(new VariableOperator(name, true), token, calc());
                }
            }
            return new VariableOperator(name, false);
        } else if (currentToken.getType() == TokenType.KEYWORD_ECHO) {
            eat(TokenType.KEYWORD_ECHO);
            return new UnaryOperator(token, calc());
        }
        if (token.getType() == TokenType.KEYWORD_TRUE) {
            eat(TokenType.KEYWORD_TRUE);
            return new NoOperator(new JPLBoolean(true), NoOpType.Boolean, token);
        } else if (token.getType() == TokenType.KEYWORD_FALSE) {
            eat(TokenType.KEYWORD_FALSE);
            return new NoOperator(new JPLBoolean(false), NoOpType.Boolean, token);
        }
        if (currentToken.getType() == TokenType.EOF) {
            return new Node();
        }
        throw new ParseException(row, column);
    }

    private Node term() {
        Node node = factor();
        while (isBOperator(currentToken.getType())) {
            Token token = currentToken;
            TokenType type = token.getType();
            eat(type);
            node = new BinaryOperator(node, token, factor());
        }

        return node;
    }

    private boolean isBOperator(TokenType type) {
        TokenType[] types = {TokenType.OP_MULTIPLY, TokenType.OP_DIVIDE, TokenType.OP_LT, TokenType.OP_LTE,
                TokenType.OP_GT, TokenType.OP_GTE, TokenType.OP_NOT_EQUAL};
        for (TokenType e : types) {
            if (e == type) {
                return true;
            }
        }
        return false;
    }

    public Node calc() throws ParseException {
        Node node = term();
        while (currentToken.getType() == TokenType.OP_PLUS || currentToken.getType() == TokenType.OP_MINUS) {
            Token token = currentToken;
            if (currentToken.getType() == TokenType.OP_PLUS) {
                eat(TokenType.OP_PLUS);
            } else if (currentToken.getType() == TokenType.OP_MINUS) {
                eat(TokenType.OP_MINUS);
            }
            node = new BinaryOperator(node, token, term());
        }

        return node;
    }
}
