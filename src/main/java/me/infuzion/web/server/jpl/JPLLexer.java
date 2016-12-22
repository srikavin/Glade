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
    private final String text;
    private Character currentChar;
    private int index;
    private int row;
    private int column;

    public JPLLexer(String text) {
        this.text = text.replaceAll("\\n", "\n");
        index = 0;
        row = 1;
        column = 1;
        currentChar = text.charAt(index);
    }

    private double getNumber() {
        String result = "";
        boolean decimalOccurred = false;
        while (currentChar != null && (Character.isDigit(currentChar) || currentChar == '.')) {
            if (currentChar == '.') {
                if (decimalOccurred) {
                    throw new ParseException(row, column, "Malformed number!");
                }
                decimalOccurred = true;
            }
            result += currentChar;
            advance();
        }
        return Double.parseDouble(result);
    }

    private Token parseAlpha() {
        String result = "";
        while (currentChar != null && Character.isAlphabetic(currentChar)) {
            result += currentChar;
            advance();
        }
        switch (result) {
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
            default:
                return new Token(TokenType.VAR_NAME, result, row, column);
        }
    }

    public Token getNextToken() throws ParseException {
        while (currentChar != null) {
            currentChar = text.charAt(index);

            if (currentChar == '#' || (currentChar == '/' && peek() != null && peek() == '*')) {
                skipComment();
                continue;
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
                String result = "";
                while (currentChar != null) {
                    if (currentChar == '\\' && peek() == null && peek() == '"') {
                        result += '"';
                        advance();
                        continue;
                    } else if (currentChar == '"') {
                        advance();
                        break;
                    }
                    result += currentChar;
                    advance();
                }
                advance();
                return new Token(TokenType.STRING_LITERAL, result, row, column);
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
                String key = "";
                while (currentChar != ']') {
                    key += currentChar;
                    advance();
                }
                advance();
                return new Token(TokenType.ARRAY_KEY, key, row, column);
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
        int peek_pos = index + 1;
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
