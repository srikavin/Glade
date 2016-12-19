package me.infuzion.web.server.parser;

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
        System.out.println(interpreter.interpret(parser));

        parser = new Parser(new JPLLexer("3  -  5 +  2"));
        System.out.println(interpreter.interpret(parser));

        parser = new Parser(new JPLLexer("3  *  5 +  2"));
        System.out.println(interpreter.interpret(parser));

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            try {
                lexer = new JPLLexer(scanner.nextLine());
                parser = new Parser(lexer);
                System.out.println(interpreter.interpret(parser));
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
        if (token.getType() == TokenType.TYPE_INTEGER) {
            eat(TokenType.TYPE_INTEGER);
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
        } else if (currentToken.getType() == TokenType.KEYWORD_VAR) {
            eat(TokenType.KEYWORD_VAR);
            if (currentToken.getType() == TokenType.VAR_NAME) {
                String name = currentToken.getValue();
                eat(TokenType.VAR_NAME);
                if (currentToken.getType() == TokenType.SEMI) {
                    token = currentToken;
                    eat(TokenType.SEMI);
                    return new Variable<>(token, name, null, factor());
                } else if (currentToken.getType() == TokenType.ASSIGN) {
                    token = currentToken;
                    eat(TokenType.ASSIGN);
                    if (currentToken.getType() == TokenType.TYPE_INTEGER || currentToken.getType() == TokenType.VAR_NAME) {
                        return new BinaryOperator(new NoOperator(name, true), token, calc());
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
                if (currentToken.getType() == TokenType.TYPE_INTEGER || currentToken.getType() == TokenType.VAR_NAME) {
                    return new BinaryOperator(new NoOperator(name, true), token, calc());
                }
            }
            return new NoOperator(name, false);
        }
        if (currentToken.getType() == TokenType.EOF) {
            return new Node();
        }
        throw new ParseException(row, column);
    }

    private Node term() {
        Node node = factor();
        while ((currentToken.getType() == TokenType.OP_MULTIPLY) || currentToken.getType() == TokenType.OP_DIVIDE
                || currentToken.getType() == TokenType.EXPONENT) {
            Token token = currentToken;
            if (token.getType() == TokenType.OP_MULTIPLY) {
                eat(TokenType.OP_MULTIPLY);
            } else if (token.getType() == TokenType.OP_DIVIDE) {
                eat(TokenType.OP_DIVIDE);
            } else if (token.getType() == TokenType.EXPONENT){
                eat(TokenType.EXPONENT);
            }

            node = new BinaryOperator(node, token, factor());
        }

        return node;
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
