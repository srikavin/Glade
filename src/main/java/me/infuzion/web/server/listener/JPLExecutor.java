/*
 *    Copyright 2018 Srikavin Ramkumar
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

package me.infuzion.web.server.listener;

import me.infuzion.web.server.EventListener;
import me.infuzion.web.server.event.EventManager;
import me.infuzion.web.server.event.def.PageRequestEvent;
import me.infuzion.web.server.event.reflect.EventHandler;
import me.infuzion.web.server.util.HttpParameters;
import me.infuzion.web.server.util.Utilities;
import me.srikavin.jpl.Interpreter;
import me.srikavin.jpl.JPLLexer;
import me.srikavin.jpl.JPLParser;
import me.srikavin.jpl.data.jpl.JPLArray;
import me.srikavin.jpl.data.jpl.JPLString;
import me.srikavin.jpl.data.node.Node;
import me.srikavin.jpl.data.node.Variable;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JPLExecutor implements EventListener {
    private final Pattern includeFunction = Pattern.compile("!\\{INCLUDE\\s?\\{\\s?(.+?)\\s?}\\s?}");
    private final Pattern calcFunction = Pattern.compile("<!jpl\\s.+? ?!>", Pattern.DOTALL);

    public JPLExecutor(EventManager eventManager) {
        eventManager.registerListener(this);
    }

    @EventHandler
    public void onPageLoad(PageRequestEvent event) {
        System.out.println(event.getPage());
        if ((event.getPage().endsWith(".jpl") && !event.isHandled()) || event.getContentType().equals("jpl")) {
            InputStream stream = getClass().getResourceAsStream("/web/" + event.getPage());
            if (event.getUrlParameters().getParameters().containsKey("noredir")) {
                event.setContentType("text");
                event.setResponseData(Utilities.convertStreamToString(stream));
                event.setStatusCode(200);
                return;
            }
            if (stream != null) {
                String content = Utilities.convertStreamToString(stream);

                content = parseVariables(content, event);

                event.setResponseData(content);
                event.setHandled(true);

            }
        }
    }

    private String parseVariables(String content, PageRequestEvent event) {
        Map<String, Variable> variableMap = new HashMap<>();
        JPLArray array = new JPLArray();
        for (Map.Entry<String, List<String>> e : event.getUrlParameters().getParameters().entrySet()) {
            array.set(e.getKey(), new JPLString(e.getValue().get(0)));
        }
        Variable get = new Variable(null, "GET", array, new Node());
        array = new JPLArray();
        for (Map.Entry<String, List<String>> e : event.getBodyParameters().getParameters().entrySet()) {
            array.set(e.getKey(), new JPLString(e.getValue().get(0)));
        }
        Variable post = new Variable(null, "POST", array, new Node());

        array = new JPLArray();
        for (Map.Entry<String, Object> e : event.getSession().entrySet()) {
            array.set(e.getKey(), new JPLString(e.getValue().toString()));
        }
        Variable session = new Variable(null, "SESSION", array, new Node());

        variableMap.put("GET", get);
        variableMap.put("POST", post);
        variableMap.put("SESSION", session);

        Matcher includeMatcher = includeFunction.matcher(content);
        content = parseIncludes(includeMatcher, content, event);

        Matcher calcMatcher = calcFunction.matcher(content);
        JPLLexer lexer;
        JPLParser parser;
        Interpreter interpreter = new Interpreter(variableMap);
        while (calcMatcher.find()) {
            lexer = new JPLLexer(calcMatcher.group(0));
            parser = new JPLParser(lexer);
            interpreter.interpret(parser);
            String result = interpreter.getOutput();
            content = content.replace(calcMatcher.group(0), result);
        }

        if (event.getStatusCode() != 500) {
            event.setStatusCode(200);
        } else {
            return "";
        }


        return content;
    }

    private String parseIncludes(Matcher matcher, String content, PageRequestEvent event) {
        while (matcher.find()) {
            String fileName = matcher.group(1);
            if (fileName.equalsIgnoreCase(event.getPage())) {
                event.setStatusCode(500);
                return "";
            }
            InputStream stream = getClass().getResourceAsStream("/web" + fileName);
            String fileContent = Utilities.convertStreamToString(stream);
            if (fileName.endsWith(".jpl")) {
                fileContent = parseVariables(fileContent, event);
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
