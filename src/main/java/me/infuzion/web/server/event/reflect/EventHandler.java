package me.infuzion.web.server.event.reflect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If using {@link EventControl#FULL}, {@code return true} if you want to retain full control or
 * {@code return false} if you want other listeners to be called.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventHandler {
    EventPriority priority() default EventPriority.NORMAL;

    EventControl control() default EventControl.NORMAL;
}
