package me.infuzion.web.server.listener;

import me.infuzion.web.server.PageLoadListener;
import me.infuzion.web.server.EventManager;
import me.infuzion.web.server.event.PageLoadEvent;
import me.infuzion.web.server.parser.JPLLexer;
import me.infuzion.web.server.parser.Parser;
import me.infuzion.web.server.parser.Interpreter;
import me.infuzion.web.server.util.HttpParameters;
import me.infuzion.web.server.util.Utilities;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateReplacer implements PageLoadListener {
    private final Pattern getVariable = Pattern.compile("!\\{GET\\s?\\{\\s?(.+?)\\s?}\\s?}");
    private final Pattern postVariable = Pattern.compile("!\\{POST\\s?\\{\\s?(.+?)\\s?}\\s?}");
    private final Pattern includeFunction = Pattern.compile("!\\{INCLUDE\\s?\\{\\s?(.+?)\\s?}\\s?}");
    private final Pattern letConstruct = Pattern.compile("!\\{\\s?LET\\s(.+?)\\s?=\\s?(.+?)\\s?\\s?}");
    private final Pattern echoFunction = Pattern.compile("!\\{ECHO\\s?\\{\\s?(.+?)\\s?}\\s?}");
    private final Pattern stringConstruct = Pattern.compile("\"(.+)\"");
    private final Pattern calcFunction = Pattern.compile("!\\{CALC\\s?\\{\\s?(.+)\\s?\\s?}}");

    private ThreadLocal<Map<String, String>> variables = ThreadLocal.withInitial(HashMap::new);

    public TemplateReplacer(EventManager eventManager) {
        eventManager.registerListener(this);
    }

    @Override
    public void onPageLoad(PageLoadEvent event) {
        System.out.println(event.getPage());
        if (event.getPage().endsWith(".jpl") && !event.isHandled()) {
            InputStream stream = getClass().getResourceAsStream("/web/" + event.getPage());
            if(event.getGetParameters().getParameters().containsKey("noredir")){
                event.setFileEncoding("text/text");
                event.setResponseData(Utilities.convertStreamToString(stream));
                event.setStatusCode(200);
                return;
            }
            if (stream != null) {
                String content = Utilities.convertStreamToString(stream);

                content = parseVariables(content, event, 0);

                event.setResponseData(content);
                event.setHandled(true);

            }
        }
    }

    private String parseVariables(String content, PageLoadEvent event, int a) {
        Matcher getMatcher = getVariable.matcher(content);
        content = parseHTTPRequest(getMatcher, content, event.getGetParameters());

        Matcher postMatcher = postVariable.matcher(content);
        content = parseHTTPRequest(postMatcher, content, event.getPostParameters());

        Matcher letMatcher = letConstruct.matcher(content);
        content = parseLetConstruct(letMatcher, content);

        Matcher echoMatcher = echoFunction.matcher(content);
        content = parseEchoFunction(echoMatcher, content);

        Matcher calcMatcher = calcFunction.matcher(content);
        JPLLexer lexer;
        Parser parser;
        while(calcMatcher.find()){
            System.out.println("Calc: " + calcMatcher.group(1));
            try {
                lexer = new JPLLexer(calcMatcher.group(1));
                parser = new Parser(lexer);
                Interpreter interpreter = new Interpreter();
                String result = interpreter.interpret(parser);
                content = content.replaceFirst(calcMatcher.pattern().toString(), result);
            } catch (IllegalArgumentException e){
                event.setStatusCode(500);
            }
        }


        if (event.getStatusCode() != 500) {
            event.setStatusCode(200);
        } else {
            return "";
        }


        Matcher includeMatcher = includeFunction.matcher(content);
        content = parseIncludes(includeMatcher, content, event);

        if(a == 0){
            parseVariables(content, event, a+1);
        }
        return content;
    }

    private String parseEchoFunction(Matcher matcher, String content){
        while (matcher.find()) {
            String varname = matcher.group(1);
            String toReplace = "";
            if(variables.get().containsKey(varname)){
                toReplace = variables.get().get(varname);
            }
            content = content.replaceFirst(matcher.pattern().toString(), toReplace);
        }
        return content;
    }

    private String parseLetConstruct(Matcher matcher, String content) {
        //parse variables
        while (matcher.find()) {
            String name = matcher.group(1);
            String value = matcher.group(2);
            content = content.replaceFirst(letConstruct.pattern(), "");
            variables.get().put(name, value);
        }
        return content;
    }

    private String parseIncludes(Matcher matcher, String content, PageLoadEvent event) {
        while (matcher.find()) {
            String fileName = matcher.group(1);
            if (fileName.equalsIgnoreCase(event.getPage())) {
                event.setStatusCode(500);
                return "";
            }
            InputStream stream = getClass().getResourceAsStream("/web" + fileName);
            String fileContent = Utilities.convertStreamToString(stream);
            if (fileName.endsWith(".jpl")) {
                fileContent = parseVariables(fileContent, event, 1);
            }
            if (event.getStatusCode() == 500) {
                return "";
            }
            if (stream == null) {
                System.out.println("3");
                event.setStatusCode(500);
                return "";
            } else {
                content = content.replaceFirst(matcher.pattern().toString(), fileContent);
            }
        }
        return content;
    }

    private String parseHTTPRequest(Matcher matcher, String content, HttpParameters parameters) {
        while (matcher.find()) {
            String var = matcher.group(1);
            if (parameters.get(var) == null || (parameters.get(var).get(0)) == null) {
                content = content.replace("!{" + parameters.getMethod() + "{" + var + "}}", "");
            } else {
                content = content.replace("!{" + parameters.getMethod() + "{" + var + "}}", parameters.get(var).get(0));
            }
        }
        return content;
    }

}
