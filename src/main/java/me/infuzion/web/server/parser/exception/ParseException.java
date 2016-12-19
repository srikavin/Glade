package me.infuzion.web.server.parser.exception;

public class ParseException extends RuntimeException {
    public ParseException(int row, int column){
        super("Error at row " + row + " column " + column);
    }
}
