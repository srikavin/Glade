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

import me.infuzion.web.server.EventManager;
import me.infuzion.web.server.PageLoadListener;
import me.infuzion.web.server.event.PageLoadEvent;
import me.infuzion.web.server.jpl.data.jpl.JPLArray;
import me.infuzion.web.server.jpl.data.jpl.JPLString;
import me.infuzion.web.server.jpl.data.node.Node;
import me.infuzion.web.server.jpl.data.node.Variable;
import me.infuzion.web.server.util.HttpParameters;
import me.infuzion.web.server.util.Utilities;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JPLExecutor implements PageLoadListener {
    private final Pattern getVariable = Pattern.compile("!\\{GET\\s?\\{\\s?(.+?)\\s?}\\s?}");
    private final Pattern postVariable = Pattern.compile("!\\{POST\\s?\\{\\s?(.+?)\\s?}\\s?}");
    private final Pattern includeFunction = Pattern.compile("!\\{INCLUDE\\s?\\{\\s?(.+?)\\s?}\\s?}");
    private final Pattern calcFunction = Pattern.compile("<!jpl\\s(.+?)\\s?!>", Pattern.DOTALL);

    public JPLExecutor(EventManager eventManager) {
        eventManager.registerListener(this);
    }

    @Override
    public void onPageLoad(PageLoadEvent event) {
        System.out.println(event.getPage());
        if (event.getPage().endsWith(".jpl") && !event.isHandled()) {
            InputStream stream = getClass().getResourceAsStream("/web/" + event.getPage());
            if (event.getGetParameters().getParameters().containsKey("noredir")) {
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
        Map<String, Variable> variableMap = new HashMap<>();
        JPLArray array = new JPLArray();
        for (Map.Entry<String, List<String>> e : event.getGetParameters().getParameters().entrySet()) {
            array.set(e.getKey(), new JPLString(e.getValue().get(0)));
        }
        Variable get = new Variable(null, "GET", array, new Node());
        array = new JPLArray();
        for (Map.Entry<String, List<String>> e : event.getPostParameters().getParameters().entrySet()) {
            array.set(e.getKey(), new JPLString(e.getValue().get(0)));
        }
        Variable post = new Variable(null, "POST", array, new Node());
        variableMap.put("GET", get);
        variableMap.put("POST", post);

        Matcher calcMatcher = calcFunction.matcher(content);
        JPLLexer lexer;
        Parser parser;
        Interpreter interpreter = new Interpreter(variableMap);
        while (calcMatcher.find()) {
            System.out.println("Calc: " + calcMatcher.group(1));
            try {
                lexer = new JPLLexer(calcMatcher.group(1));
                parser = new Parser(lexer);
                interpreter.interpret(parser);
                String result = interpreter.getOutput();
                content = content.replaceFirst(calcMatcher.pattern().toString(), result);
            } catch (IllegalArgumentException e) {
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

        if (a == 0) {
            parseVariables(content, event, a + 1);
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
