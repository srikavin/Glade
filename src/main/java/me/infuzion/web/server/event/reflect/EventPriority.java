package me.infuzion.web.server.event.reflect;

public enum EventPriority {
    /**
     * Called before all other event priorities
     */
    START(-1),
    /**
     * Called after {@link #START}; before {@link #END} and {@link #MONITOR}
     */
    NORMAL(100),
    /**
     * Called before {@link #MONITOR}
     */
    END(200),
    /**
     * Called after all other event priorities. Should only be used to monitor results and logging purposes
     */
    MONITOR(1000);

    private final int priority;

    EventPriority(int priority) {
        this.priority = priority;
    }

    public static int compare(EventPriority first, EventPriority other) {
        return first.priority - other.priority;
    }
}
