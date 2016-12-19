package me.infuzion.web.server.parser;

public enum TokenType {
    KEYWORD_IF,
    KEYWORD_BEGIN,
    KEYWORD_END,
    ASSIGN,
    SEMI,
    KEYWORD_ELSE,
    KEYWORD_VAR,
    VAR_NAME,
    KEYWORD_ECHO,
    TYPE_INTEGER,
    OP_PLUS,
    OP_MINUS,
    OP_MULTIPLY,
    OP_DIVIDE,
    PARENTHESIS_LEFT,
    PARENTHESIS_RIGHT,
    EXPONENT,
    EOF
}
