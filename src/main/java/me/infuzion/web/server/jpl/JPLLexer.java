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

package me.infuzion.web.server.jpl;

import me.infuzion.web.server.jpl.exception.ParseException;

public class JPLLexer {

    private static final char[] escapedCharacters = {'n', '"'};
    private final String text;
    private Character currentChar;
    private int index;
    private int row;
    private int column;
    private boolean inJPLTag = false;

    public JPLLexer(String text) {
        this.text = text.replaceAll("\\n", "\n");
        index = 0;
        row = 1;
        column = 1;
        currentChar = text.charAt(index);
    }

    private double getNumber() {
        StringBuilder result = new StringBuilder();
        boolean decimalOccurred = false;
        while (currentChar != null && (Character.isDigit(currentChar) || currentChar == '.')) {
            if (currentChar == '.') {
                if (decimalOccurred) {
                    throw new ParseException(row, column, "Malformed number!");
                }
                decimalOccurred = true;
            }
            result.append(currentChar);
            advance();
        }
        return Double.parseDouble(result.toString());
    }

    private Token parseAlpha() {
        StringBuilder result = new StringBuilder();
        while (currentChar != null && Character.isAlphabetic(currentChar)) {
            result.append(currentChar);
            advance();
        }
        switch (result.toString()) {
            case "if":
                return new Token(TokenType.KEYWORD_IF, "if", row, column);
            case "echo":
                return new Token(TokenType.KEYWORD_ECHO, "echo", row, column);
            case "var":
                return new Token(TokenType.KEYWORD_VAR, "var", row, column);
            case "true":
                return new Token(TokenType.KEYWORD_TRUE, "true", row, column);
            case "false":
                return new Token(TokenType.KEYWORD_FALSE, "false", row, column);
            case "while":
                return new Token(TokenType.KEYWORD_WHILE, "while", row, column);
            case "for":
                return new Token(TokenType.KEYWORD_FOR, "for", row, column);
            case "else":
                return new Token(TokenType.KEYWORD_ELSE, "else", row, column);
            case "elif":
                return new Token(TokenType.KEYWORD_ELSE_IF, "elif", row, column);
            default:
                return new Token(TokenType.VAR_NAME, result.toString(), row, column);
        }
    }

    private char parseEscapedChars() {
        Character peek = peek();
        if (peek == null) {
            throw new ParseException(row, column, "Unexpected end of file!");
        }
        for (char e : escapedCharacters) {
            if (e == peek) {
                advance();
                advance();
                return e;
            }
        }
        throw new ParseException(row, column, "Unknown escape sequence! " + currentChar);
    }

    private void skipNonJPL() {
        while (currentChar != null) {
            if (!inJPLTag && currentChar == '<') {
                Character excl = peek(1);
                if (excl == null || excl != '!') {
                    continue;
                }
                Character j = peek(2);
                if (j == null || j != 'j') {
                    continue;
                }
                Character p = peek(3);
                if (p == null || p != 'p') {
                    continue;
                }
                Character l = peek(4);
                if (l == null || l != 'l') {
                    continue;
                }
                advance();
                advance();
                advance();
                advance();
                advance();
                skipWhiteSpace();
                inJPLTag = true;
                return;
            }
            if (inJPLTag && currentChar == '!' && peek() != null && peek() == '>') {
                advance();
                advance();
                inJPLTag = false;
            }
            if (inJPLTag) {
                return;
            }
            advance();
        }
    }

    public Token getNextToken() throws ParseException {
        while (currentChar != null) {
            currentChar = text.charAt(index);

            skipNonJPL();

            if (currentChar == null) {
                return new Token(TokenType.EOF, "", row, column);
            }

            if (currentChar == '#' || (currentChar == '/' && peek() != null && peek() == '*')) {
                skipComment();
                continue;
            }

            if (currentChar == '\\') {
                return new Token(TokenType.LITERAL, "\\" + parseEscapedChars(), row, column);
            }

            if (currentChar == '\r' && peek() != null && peek() == '\n') {
                advance();
                advance();
                row++;
                column = 0;
                skipWhiteSpace();
                continue;
            }

            if (currentChar == '\r' || currentChar == '\n') {
                advance();
                skipWhiteSpace();
            }

            if (Character.isSpaceChar(currentChar)) {
                skipWhiteSpace();
                continue;
            }

            if (Character.isAlphabetic(currentChar)) {
                return parseAlpha();
            }

            if (currentChar == ';') {
                advance();
                return new Token(TokenType.SEMI, ";", row, column);
            }

            if (currentChar == '?') {
                advance();
                return new Token(TokenType.TERNARY_START, "?", row, column);
            }

            if (currentChar == ':') {
                advance();
                return new Token(TokenType.TERNARY_SEPERATOR, ":", row, column);
            }

            if (currentChar == '<') {
                advance();
                if (currentChar == '=') {
                    advance();
                    return new Token(TokenType.OP_LTE, "<=", row, column);
                }
                return new Token(TokenType.OP_LT, "<", row, column);
            }

            if (currentChar == '>') {
                advance();
                if (currentChar == '=') {
                    advance();
                    return new Token(TokenType.OP_GTE, ">=", row, column);
                }
                return new Token(TokenType.OP_GT, ">", row, column);
            }

            if (currentChar == '"') {
                advance();
                StringBuilder result = new StringBuilder();
                while (currentChar != null) {
                    if (currentChar == '\\') {
                        switch (parseEscapedChars()) {
                            case 'n':
                                result.append('\n');
                                break;
                            case '"':
                                result.append('"');
                                break;
                        }
                        continue;
                    } else if (currentChar == '"') {
                        advance();
                        break;
                    }
                    result.append(currentChar);
                    advance();
                }
                advance();
                return new Token(TokenType.STRING_LITERAL, result.toString(), row, column);
            }

            if (currentChar == '!') {
                advance();
                if (currentChar == '=') {
                    advance();
                    return new Token(TokenType.OP_NOT_EQUAL, "!=", row, column);
                }
                return new Token(TokenType.OP_NOT, "!", row, column);
            }

            if (currentChar == '{') {
                advance();
                if (currentChar == '}') {
                    advance();
                    return new Token(TokenType.ARRAY_INITIALIZER, "{}", row, column);
                }
                return new Token(TokenType.CURLY_BRACKET_LEFT, "{", row, column);
            }

            if (currentChar == '[') {
                advance();
                StringBuilder key = new StringBuilder();
                while (currentChar != ']') {
                    key.append(currentChar);
                    advance();
                }
                advance();
                return new Token(TokenType.ARRAY_KEY, key.toString(), row, column);
            }

            if (currentChar == '}') {
                advance();
                return new Token(TokenType.CURLY_BRACKET_RIGHT, "}", row, column);
            }

            if (currentChar == '=') {
                advance();
                if (currentChar == '=') {
                    advance();
                    return new Token(TokenType.OP_EQUALS, "==", row, column);
                }
                return new Token(TokenType.ASSIGN, "=", row, column);
            }

            if (currentChar == '^') {
                advance();
                return new Token(TokenType.OP_EXPONENT, "^", row, column);
            }

            if (currentChar == '+') {
                advance();
                return new Token(TokenType.OP_PLUS, "+", row, column);
            }

            if (currentChar == '-') {
                advance();
                return new Token(TokenType.OP_MINUS, "-", row, column);
            }

            if (currentChar == '*') {
                advance();
                return new Token(TokenType.OP_MULTIPLY, "*", row, column);
            }

            if (currentChar == '/') {
                advance();
                return new Token(TokenType.OP_DIVIDE, "/", row, column);
            }

            if (currentChar == '.') {
                advance();
                return new Token(TokenType.STRING_CONCATENATE, ".", row, column);
            }

            if (currentChar == '(') {
                advance();
                return new Token(TokenType.PARENTHESIS_LEFT, "(", row, column);
            }

            if (currentChar == ')') {
                advance();
                return new Token(TokenType.PARENTHESIS_RIGHT, ")", row, column);
            }

            if (Character.isDigit(currentChar)) {
                return new Token(TokenType.TYPE_NUMBER, String.valueOf(getNumber()), row, column);
            }

            if (currentChar != null) {
                throw new ParseException(row, column, "Unknown token: \"" + currentChar + "\"");
            }
            throw new ParseException(row, column, "Unexpected end of input!");
        }
        return new Token(TokenType.EOF, "", row, column);
    }

    private void skipComment() {
        if (currentChar == '#') {
            while (currentChar != null && currentChar != '\n') {
                advance();
            }
        } else if (currentChar == '/' && peek() != null && peek() == '*') {
            advance();
            advance();
            while (currentChar != null) {
                advance();
                if (currentChar == '*') {
                    advance();
                    if (currentChar == '/') {
                        advance();
                        break;
                    }
                }
            }
        }
    }

    private Character peek() {
        return peek(1);
    }

    private Character peek(int amount) {
        int peek_pos = index + amount;
        if (peek_pos > text.length() - 1) {
            return null;
        } else {
            return text.charAt(peek_pos);
        }
    }

    private void advance() {
        index++;
        column++;
        if (index > text.length() - 1) {
            currentChar = null;
        } else {
            currentChar = text.charAt(index);
        }
    }

    private void skipWhiteSpace() {
        while (currentChar != null && Character.isSpaceChar(currentChar)) {
            column++;
            advance();

        }
    }
}
