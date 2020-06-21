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

package me.infuzion.web.server.event;

import com.google.common.flogger.FluentLogger;
import me.infuzion.web.server.EventListener;
import me.infuzion.web.server.event.reflect.EventControl;
import me.infuzion.web.server.event.reflect.EventHandler;
import me.infuzion.web.server.event.reflect.EventPriority;
import me.infuzion.web.server.event.reflect.param.mapper.EventPredicate;
import me.infuzion.web.server.event.reflect.param.mapper.InvalidEventConfiguration;
import me.infuzion.web.server.event.reflect.param.mapper.ParamMapper;
import me.infuzion.web.server.event.reflect.param.mapper.ResponseMapper;
import me.infuzion.web.server.util.Pair;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public class EventManager {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    private static class EventListenerData implements Comparable<EventListenerData> {
        public EventListenerData(EventListener instance,
                                 Class<? extends Event> eventType,
                                 EventPriority priority,
                                 EventControl control,
                                 Method method,
                                 Parameter[] parameters,
                                 Pair<ParamMapper<? extends Annotation, ?, ?>, Annotation>[] paramMapper,
                                 Pair<ResponseMapper<? extends Annotation, ?, ?>, Annotation> responseMapper,
                                 List<Pair<EventPredicate<? extends Annotation, ?>, Annotation>> eventPredicates) {
            this.instance = instance;
            this.eventType = eventType;
            this.priority = priority;
            this.control = control;
            this.method = method;
            this.parameters = parameters;
            this.paramMapper = paramMapper;
            this.responseMapper = responseMapper;
            this.eventPredicates = eventPredicates;
        }

        EventListener instance;

        Class<? extends Event> eventType;

        EventPriority priority;
        EventControl control;

        Method method;
        Parameter[] parameters;

        Pair<ParamMapper<? extends Annotation, ?, ?>, Annotation>[] paramMapper;
        Pair<ResponseMapper<? extends Annotation, ?, ?>, Annotation> responseMapper;

        List<Pair<EventPredicate<? extends Annotation, ?>, Annotation>> eventPredicates;

        @Override
        public int compareTo(EventListenerData other) {
            int val = this.priority.getValue() - other.priority.getValue();

            if (val == 0) {
                return this.control.getValue() - other.control.getValue();
            }

            return val;
        }
    }

    private final Map<Class<? extends Event>, List<EventListenerData>> listeners = new HashMap<>();

    private final Map<Class<? extends Annotation>, ParamMapper<?, ?, ?>> registeredParamMappers = new HashMap<>();
    private final Map<Class<? extends Annotation>, ResponseMapper<?, ?, ?>> registeredResponseMappers = new HashMap<>();
    private final Map<Class<? extends Annotation>, EventPredicate<?, ?>> registeredEventPredicates = new HashMap<>();

    private final List<ParamMapper<? extends Annotation, ?, ?>> defaultParamMappers = new ArrayList<>();
    private final List<ResponseMapper<? extends Annotation, ?, ?>> defaultResponseMappers = new ArrayList<>();
    private final List<EventPredicate<? extends Annotation, ?>> defaultEventPredicates = new ArrayList<>();

    public EventManager() {
        this.defaultParamMappers.add(new ParamMapper<>() {
            @Override
            public Object map(Annotation annotation, Method method, Class<?> parameterType, Event event) {
                return event;
            }

            @Override
            public boolean validate(Annotation annotation, Method method, Class<?> parameterType, Class<? extends Event> event) {
                return event.isAssignableFrom(parameterType);
            }
        });
    }

    public <A extends Annotation> void registerAnnotation(Class<A> annotationType, ParamMapper<A, ? extends Event, ?> mapper) {
        registeredParamMappers.put(annotationType, mapper);
    }

    public <A extends Annotation> void registerAnnotation(Class<A> annotationType, ResponseMapper<A, ? extends Event, ?> mapper) {
        registeredResponseMappers.put(annotationType, mapper);
    }

    public <A extends Annotation> void registerAnnotation(Class<A> annotationType, EventPredicate<A, ? extends Event> predicate) {
        registeredEventPredicates.put(annotationType, predicate);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void fireEvent(Event event) {
        Class eventClass = event.getClass();

        logger.atFiner().log("%s (%s) event fired", event, eventClass);

        List<EventListenerData> listenerDataList = new ArrayList<>();

        while (Event.class.isAssignableFrom(eventClass)) {
            List<EventListenerData> temp = listeners.get(eventClass);

            if (temp != null) {
                listenerDataList.addAll(temp);
            }

            eventClass = eventClass.getSuperclass();
        }

        if (listenerDataList.isEmpty()) {
            // no registered listeners
            return;
        }

        outer:
        for (EventListenerData listenerData : listenerDataList) {
            // check predicates
            for (var e : listenerData.eventPredicates) {
                EventPredicate predicate = e.left;

                if (!predicate.shouldCall(e.right, event)) {
                    logger.atFiner().log("%s call prevented by predicate %s", listenerData.method, predicate.getClass());
                    predicate.onCallPrevented(e.right, event);
                    continue outer;
                }
            }

            for (EventPredicate predicate : defaultEventPredicates) {
                if (!predicate.shouldCall(null, event)) {
                    predicate.onCallPrevented(null, event);
                    continue outer;
                }
            }

            Object[] params = new Object[listenerData.parameters.length];

            var paramMappers = listenerData.paramMapper;
            for (int i = 0; i < paramMappers.length; i++) {
                var mapperPair = paramMappers[i];
                ParamMapper mapper = mapperPair.left;

                params[i] = mapper.map(mapperPair.right, listenerData.method, listenerData.parameters[i].getType(), event);
            }

            try {
                Object o = listenerData.method.invoke(listenerData.instance, params);

                if (listenerData.control == EventControl.FULL) {
                    // take care of autoboxing
                    Boolean tookControl = (Boolean) o;
                    if (tookControl) {
                        break;
                    }
                }

                if (listenerData.responseMapper != null) {
                    ResponseMapper responseMapper = listenerData.responseMapper.left;

                    responseMapper.map(listenerData.responseMapper.right, listenerData.method, event, o);
                } else if (o != null) {
                    for (ResponseMapper e : defaultResponseMappers) {
                        if (e.validate(null, listenerData.method, o.getClass(), event.getClass())) {
                            e.map(null, listenerData.method, event, o);
                        }
                    }
                }

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private <A extends Annotation> ParamMapper<A, ?, ?> getParamMapper(Class<A> annotationClass) {
        //noinspection unchecked
        return (ParamMapper<A, ?, ?>) this.registeredParamMappers.get(annotationClass);
    }

    private <A extends Annotation> ResponseMapper<A, ?, ?> getResponseMapper(Class<A> annotationClass) {
        //noinspection unchecked
        return (ResponseMapper<A, ?, ?>) this.registeredResponseMappers.get(annotationClass);
    }

    private <A extends Annotation> EventPredicate<A, ?> getEventPredicate(Class<A> annotationClass) {
        //noinspection unchecked
        return (EventPredicate<A, ?>) this.registeredEventPredicates.get(annotationClass);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Pair<ParamMapper<?, ?, ?>, Annotation>[] getParamMappers(Class<? extends Event> eventType, Method method, Parameter[] parameters) {
        //noinspection unchecked
        Pair<ParamMapper<?, ?, ?>, Annotation>[] ret = new Pair[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];

            Pair<ParamMapper<? extends Annotation, ?, ?>, Annotation> mapper = null;

            Annotation[] annotations = parameter.getAnnotations();

            for (Annotation annotation : annotations) {
                Class<? extends Annotation> annotationClass = annotation.annotationType();

                if (registeredParamMappers.containsKey(annotationClass)) {
                    ParamMapper cur = getParamMapper(annotationClass);

                    if (mapper != null) {
                        throw new InvalidEventConfiguration("Conflicting param mappers " + mapper
                                + " and " + cur, annotation, method);
                    }

                    if (!cur.validate(annotation, method, parameter.getType(), eventType)) {
                        throw new InvalidEventConfiguration(annotation, method);
                    }

                    mapper = new Pair<>(cur, annotation);
                }
            }

            if (mapper == null) {
                for (var defaultParamMapper : defaultParamMappers) {
                    if (defaultParamMapper.validate(null, method, parameter.getType(), eventType)) {
                        mapper = new Pair<>(defaultParamMapper, null);
                    }
                }

                if (mapper == null) {
                    throw new InvalidEventConfiguration("No parameter mappers found!", parameter, method);
                }
            }

            ret[i] = mapper;
        }

        return ret;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void attemptRegisterMethod(EventListener listener, Method method) {
        EventHandler markerAnnotation = method.getAnnotation(EventHandler.class);

        if (markerAnnotation == null) {
            return;
        }

        method.setAccessible(true);

        EventControl control = markerAnnotation.control();
        EventPriority priority = markerAnnotation.priority();
        Class<? extends Event> eventClass = markerAnnotation.value();

        Annotation[] annotations = method.getAnnotations();
        Parameter[] parameters = method.getParameters();

        Class<?> returnType = method.getReturnType();

        if (control == EventControl.FULL) {
            if (returnType != boolean.class && returnType != Boolean.class) {
                throw new InvalidEventConfiguration("Event takes full control, but does not return boolean!", method);
            }
        }

        if (eventClass.equals(EventHandler.NoEventSelected.class)) {
            // the first parameter is the event type
            if (parameters.length == 0) {
                throw new InvalidEventConfiguration("First parameter is not an event!", method);
            }

            Parameter p = parameters[0];

            if (!Event.class.isAssignableFrom(p.getType())) {
                throw new InvalidEventConfiguration("First parameter is not an event!", method);
            }

            //noinspection unchecked
            eventClass = (Class<? extends Event>) p.getType();
        }


        var paramMapper = getParamMappers(eventClass, method, parameters);

        Pair<ResponseMapper<? extends Annotation, ?, ?>, Annotation> responseMapper = null;
        List<Pair<EventPredicate<? extends Annotation, ?>, Annotation>> eventPredicates = new ArrayList<>(3);


        for (Annotation annotation : annotations) {
            Class<? extends Annotation> annotationClass = annotation.annotationType();

            if (registeredResponseMappers.containsKey(annotationClass)) {
                ResponseMapper cur = getResponseMapper(annotationClass);

                if (responseMapper != null) {
                    throw new InvalidEventConfiguration("Conflicting response mappers " + responseMapper
                            + " and " + cur, annotation, method);
                }

                if (!cur.validate(annotation, method, returnType, eventClass)) {
                    throw new InvalidEventConfiguration(annotation, method);
                }

                responseMapper = new Pair<>(cur, annotation);
            }

            if (registeredEventPredicates.containsKey(annotationClass)) {
                EventPredicate eventPredicate = getEventPredicate(annotationClass);

                if (!eventPredicate.validate(annotation, eventClass)) {
                    throw new InvalidEventConfiguration(annotation, method);
                }

                eventPredicates.add(new Pair<>(eventPredicate, annotation));
            }
        }

        logger.atInfo().log("Event listener %s registered", method);

        // sort in descending order
        eventPredicates.sort(Comparator.comparing(o -> o.left));

        EventListenerData data = new EventListenerData(listener, eventClass, priority, control, method, parameters, paramMapper, responseMapper, eventPredicates);

        if (listeners.containsKey(eventClass)) {
            List<EventListenerData> list = listeners.get(eventClass);
            list.add(data);
            list.sort(Collections.reverseOrder());

        } else {
            List<EventListenerData> listenerDataList = new ArrayList<>(15);
            listenerDataList.add(data);
            listeners.put(eventClass, listenerDataList);
        }
    }

    public void registerListener(EventListener listener) {
        Method[] methods = listener.getClass().getDeclaredMethods();

        for (Method method : methods) {
            attemptRegisterMethod(listener, method);
        }
    }
}
