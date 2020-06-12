/*
 * Copyright 2020 Srikavin Ramkumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.infuzion.web.server.event.reflect.param;

import me.infuzion.web.server.event.Event;
import me.infuzion.web.server.event.RequestEvent;
import me.infuzion.web.server.http.parser.BodyData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This class handles annotations on method parameters and the {@linkplain Response} parameter on methods. It uses these
 * parameters to generate the parameters to call a given method with. Upon creation, the method is verified to ensure
 * that errors with its definition are caught early.
 */
public class ParameterGenerator {
    private final TypeConverter converter;
    private final List<ListenerParameter> listenerParameters = new ArrayList<>();
    private String responseContentType = null;
    private boolean responseRaw = false;

    /**
     * Creates a new ListenerParameterGenerator for the given method using the given TypeConverter. The method will be
     * verified during creation of this object to catch errors early in initialization. Any caught errors will result
     * in a runtime error be thrown.
     *
     * @param converter The type converter to handle conversions to/from Strings/Objects
     * @param method    The method to generate parameters for
     */
    public ParameterGenerator(TypeConverter converter, Method method) {
        this.converter = converter;
        Response response = method.getAnnotation(Response.class);
        if (response != null) {
            responseContentType = response.value();
            responseRaw = response.raw();
            if (responseRaw && !method.getReturnType().equals(byte[].class) && !method.getReturnType().equals(ByteBuffer.class)) {
                throw new RuntimeException("Return type is not a byte[] or a ByteBuffer");
            }
        }
        addParameters(method.getParameters());
    }

    private void addParameters(Parameter[] params) {
        for (Parameter p : params) {
            Annotation found = null;
            for (Annotation e : p.getAnnotations()) {
                if (e instanceof BodyParam || e instanceof QueryParam || e instanceof URLParam) {
                    if (found != null) {
                        throw new RuntimeException("Multiple conflicting annotations found while processing method " + p.getName());
                    }
                    found = e;
                }
            }

            if (found == null) {
                if (!Event.class.isAssignableFrom(p.getType())) {
                    throw new RuntimeException("Parameter " + p.getName() + " has no annotation with its source.");
                }
                continue;
            }

            if (found instanceof BodyParam) {
                addParameter(p.getType(), (BodyParam) found);
            }

            if (found instanceof QueryParam) {
                addParameter(p.getType(), (QueryParam) found);
            }

            if (found instanceof URLParam) {
                addParameter(p.getType(), (URLParam) found);
            }
        }
    }

    public void handleReturnValue(RequestEvent event, Object ret) {
        if (responseContentType == null) {
            return;
        }

        if (!responseContentType.equals(Response.UNALTERED_CONTENT_TYPE)) {
            event.setContentType(responseContentType);
        }

        if (responseRaw) {
            if (ret instanceof byte[]) {
                event.setBody(ByteBuffer.wrap((byte[]) ret));
            } else if (ret instanceof ByteBuffer) {
                event.setBody((ByteBuffer) ret);
            } else {
                throw new RuntimeException("Returned object is not a byte[] or a ByteBuffer");
            }
        } else {
            event.setBody(converter.serialize(ret));
        }
    }

    private void addParameter(Class<?> type, QueryParam param) {
        this.listenerParameters.add(new ListenerParameter(type, ParameterType.QUERY, param.value(), false));
    }

    private void addParameter(Class<?> type, URLParam param) {
        this.listenerParameters.add(new ListenerParameter(type, ParameterType.URL, param.value(), false));
    }

    private void addParameter(Class<?> type, BodyParam param) {
        if (param.raw() && !param.value().equals(BodyParam.ENTIRE_BODY)) {
            if (!type.isAssignableFrom(BodyData.BodyField.class)) {
                throw new RuntimeException("Parameters with @Body(raw=true) must have BodyField as its type!");
            }
        }
        if (param.raw() && param.value().equals(BodyParam.ENTIRE_BODY)) {
            if (!type.isAssignableFrom(ByteBuffer.class)) {
                throw new RuntimeException("Parameters with @Body(value=ENTIRE_BODY, raw=true) must have ByteBuffer as its type!");
            }
        }
        this.listenerParameters.add(new ListenerParameter(type, ParameterType.BODY, param.value(), param.raw()));
    }

    private Object convert(String data, @Nullable ByteBuffer raw, ListenerParameter e) {
        if (e.raw && raw != null) {
            return raw;
        }
        return this.converter.deserialize(data, e.parameterType);
    }

    private Object generateBodyParameter(RequestEvent event, ListenerParameter e) {
        BodyData bodyData = event.getBodyData();
        if (Objects.equals(e.id, BodyParam.ENTIRE_BODY)) {
            return convert(event.getRequestData(), event.getRawRequestData(), e);
        }

        BodyData.BodyField field = bodyData.getFields().get(e.id);

        if (field == null) {
            return null;
        }

        if (e.raw) {
            return field;
        }

        return this.converter.deserialize(field.getContent(), e.parameterType);
    }

    private Object generateQueryParameter(RequestEvent event, ListenerParameter e) {
        if (e.parameterType.isArray()) {
            String[] arr = event.getQueryParameters().get(e.id).toArray(new String[]{});
            Object[] deserialized = new Object[arr.length];
            for (int i = 0; i < arr.length; i++) {
                String queryElement = arr[i];
                deserialized[i] = this.converter.deserialize(queryElement, e.parameterType.getComponentType());
            }
            return deserialized;
        }

        String val = null;
        List<String> queryParams = event.getQueryParameters().get(e.id);
        if (queryParams.size() > 0) {
            val = queryParams.get(queryParams.size() - 1);
        }
        return this.converter.deserialize(val, e.parameterType);
    }

    /**
     * Generates an array containing parameters to call the given method with. The first parameter will always be the
     * event.
     *
     * @param event  The event to use when generating parameter values
     * @param dynSeg A map containing all dynamic URL segments
     * @return An array that can be used to invoke th associated method uing {@link Method#invoke(Object, Object...)}
     */
    public Object[] generateMethodParameters(RequestEvent event, Map<String, String> dynSeg) {
        Object[] params = new Object[listenerParameters.size() + 1];
        params[0] = event;

        for (int i = 0; i < listenerParameters.size(); i++) {
            ListenerParameter e = listenerParameters.get(i);

            if (e.sourceType == ParameterType.BODY) {
                params[i + 1] = generateBodyParameter(event, e);
            }

            if (e.sourceType == ParameterType.URL) {
                params[i + 1] = this.converter.deserialize(dynSeg.get(e.id), e.parameterType);
            }

            if (e.sourceType == ParameterType.QUERY) {
                params[i + 1] = generateQueryParameter(event, e);
            }
        }

        return params;
    }

    private static class ListenerParameter {
        private final boolean raw;
        @NotNull
        ParameterType sourceType;
        @Nullable
        String id;
        @NotNull
        Class<?> parameterType;

        private ListenerParameter(@NotNull Class<?> parameterType, @NotNull ParameterType sourceType, @Nullable String id, boolean raw) {
            this.sourceType = sourceType;
            this.id = id;
            this.parameterType = parameterType;
            this.raw = raw;
        }
    }
}
