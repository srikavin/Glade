package me.infuzion.web.server.parser;

public enum TokenType {
    KEYWORD_IF,            // if
    KEYWORD_BEGIN,
    KEYWORD_END,
    KEYWORD_ELSE,          // else
    KEYWORD_VAR,           // var
    KEYWORD_ECHO,          // echo
    VAR_NAME,              // any alphabetic word (other than other keywords)
    ASSIGN,                // =
    SEMI,                  // ;
    KEYWORD_TRUE,          // true
    KEYWORD_FALSE,         // false
    BRACKET_LEFT,          // {
    BRACKET_RIGHT,         // }
    TYPE_NUMBER,
    STRING_CONCATENATE,    // .
    OP_PLUS,               // +
    OP_MINUS,              // -
    OP_MULTIPLY,           // *
    OP_DIVIDE,             // /
    OP_LT,                 // <
    OP_LTE,                // <=
    OP_GT,                 // >
    OP_GTE,                // >=
    OP_NOT,                // !
    OP_NOT_EQUAL,          // !=
    OP_EQUALS,             // ==
    OP_EXPONENT,           // ^
    PARENTHESIS_LEFT,      // (
    PARENTHESIS_RIGHT,     // )
    EOF,
}

// Line Comment:          #
// Block Comment:         /* ... */
