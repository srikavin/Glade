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

package me.infuzion.web.server.event.reflect.param.mapper;

import me.infuzion.web.server.EventListener;
import me.infuzion.web.server.event.AbstractEvent;
import me.infuzion.web.server.event.Event;
import me.infuzion.web.server.event.EventManager;
import me.infuzion.web.server.event.reflect.EventControl;
import me.infuzion.web.server.event.reflect.EventHandler;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class EventManagerTest {
    static class TestEvent extends AbstractEvent {
        int value = 0;

        public TestEvent() {
        }

        @Override
        public String toString() {
            return "TestEvent{" +
                    "value=" + value +
                    '}';
        }

        public TestEvent(int value) {
            this.value = value;
        }
    }

    @Test
    void basicListeners() {
        class BasicListener1 implements EventListener {
            int basicListenerCalled = 0;

            @EventHandler(TestEvent.class)
            public void basicListener() {
                System.out.println("a3");
                basicListenerCalled++;
            }

            int legacyListenerCalled = 0;

            @EventHandler
            public void legacyListener(TestEvent event) {
                System.out.println("a2");
                assertNotNull(event);
                legacyListenerCalled++;
            }

            int fullControlCalled = 0;

            @EventHandler(value = TestEvent.class, control = EventControl.FULL)
            public boolean fullControl() {
                System.out.println("a1");
                fullControlCalled++;
                assertTrue(fullControlCalled > basicListenerCalled, "full control was not called first");
                return false;
            }

        }

        class BasicListener2 implements EventListener {
            int fullControlCalled = 0;

            @EventHandler(value = TestEvent.class, control = EventControl.FULL)
            public boolean fullControl() {
                System.out.println("a");
                fullControlCalled++;
                return true;
            }
        }

        BasicListener1 listener = new BasicListener1();

        EventManager eventManager = new EventManager();
        eventManager.registerListener(listener);

        eventManager.fireEvent(new TestEvent());

        assertEquals(1, listener.basicListenerCalled);
        assertEquals(1, listener.legacyListenerCalled);


        BasicListener2 listener2 = new BasicListener2();
        eventManager.registerListener(listener2);

        eventManager.fireEvent(new TestEvent());

        // full control should prevent other handlers from running
        assertEquals(1, listener.basicListenerCalled);
        assertEquals(1, listener.legacyListenerCalled);
        assertEquals(1, listener2.fullControlCalled);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface TestAnnotation {

    }

    @Test
    void eventPredicateTest() {
        EventManager eventManager = new EventManager();

        eventManager.registerAnnotation(TestAnnotation.class, new EventPredicate<TestAnnotation, TestEvent>() {
            @Override
            public boolean shouldCall(TestAnnotation annotation, TestEvent event) {
                System.out.println(event);
                return event.value > 10;
            }

            @Override
            public boolean validate(TestAnnotation annotation, Class<? extends Event> event) {
                return TestEvent.class.isAssignableFrom(event);
            }
        });

        class TestListener implements EventListener {
            int called = 0;

            @EventHandler(TestEvent.class)
            @TestAnnotation
            public void test() {
                called++;
            }
        }

        class InvalidTestListener implements EventListener {
            @EventHandler(Event.class)
            @TestAnnotation
            public void test() {
            }
        }

        TestListener listener = new TestListener();
        eventManager.registerListener(listener);

        eventManager.fireEvent(new TestEvent(0));

        assertEquals(0, listener.called);

        eventManager.fireEvent(new TestEvent(20));
        assertEquals(1, listener.called);

        assertThrows(InvalidEventConfiguration.class, () -> eventManager.registerListener(new InvalidTestListener()));
    }


    @Test
    public void paramMapperTest() {
        EventManager eventManager = new EventManager();

        eventManager.registerAnnotation(TestAnnotation.class, new ParamMapper<TestAnnotation, TestEvent, Object>() {
            @Override
            public Object map(TestAnnotation annotation, Method method, Class<?> parameterType, TestEvent event) {
                return event.value;
            }

            @Override
            public boolean validate(TestAnnotation annotation, Method method, Class<?> parameterType, Class<? extends Event> event) {
                return TestEvent.class.isAssignableFrom(event);
            }
        });

        class TestListener implements EventListener {
            int mappedValue = 0;
            int mappedValue2 = 0;
            int mappedValue3 = 0;

            @EventHandler(TestEvent.class)
            public void asd(@TestAnnotation int value, @TestAnnotation int value2, @TestAnnotation int value3) {
                mappedValue = value;
                mappedValue2 = value2;
                mappedValue3 = value3;
            }
        }

        class InvalidTestListener implements EventListener {
            @EventHandler(Event.class)
            public void asd(@TestAnnotation int value, @TestAnnotation int value2, @TestAnnotation int value3) {
            }
        }

        TestListener listener = new TestListener();

        eventManager.registerListener(listener);

        eventManager.fireEvent(new TestEvent(20));

        assertEquals(20, listener.mappedValue);
        assertEquals(20, listener.mappedValue2);
        assertEquals(20, listener.mappedValue3);

        assertThrows(InvalidEventConfiguration.class, () -> eventManager.registerListener(new InvalidTestListener()));
    }

    @Test
    public void responseMapperTest() {
        EventManager eventManager = new EventManager();

        eventManager.registerAnnotation(TestAnnotation.class, new ResponseMapper<TestAnnotation, TestEvent, Integer>() {
            @Override
            public void map(TestAnnotation annotation, Method method, TestEvent event, Integer returnValue) {
                event.value = returnValue;
            }

            @Override
            public boolean validate(TestAnnotation annotation, Method method, Class<?> returnType, Class<? extends Event> event) {
                return TestEvent.class.isAssignableFrom(event) && (int.class.isAssignableFrom(returnType) || Integer.class.isAssignableFrom(returnType));
            }
        });

        class TestListener implements EventListener {
            @EventHandler(TestEvent.class)
            @TestAnnotation
            public int asd() {
                return 30;
            }
        }

        class InvalidTestListener implements EventListener {
            @EventHandler(Event.class)
            @TestAnnotation
            public void asd() {
            }
        }

        TestListener listener = new TestListener();

        eventManager.registerListener(listener);


        TestEvent event = new TestEvent(20);
        eventManager.fireEvent(event);

        assertEquals(30, event.value);

        assertThrows(InvalidEventConfiguration.class, () -> eventManager.registerListener(new InvalidTestListener()));
    }
}