package tn.naizo.jauml.event;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class EventTest {

    private static Event<Consumer<List<String>>> newEvent() {
        return Event.create(listeners -> log -> listeners.forEach(listener -> listener.accept(log)));
    }

    @Test
    public void testInvokerWithNoListenersDoesNothing() {
        Event<Consumer<List<String>>> event = newEvent();
        List<String> log = new ArrayList<>();
        event.invoker().accept(log);
        assertTrue(log.isEmpty());
        assertEquals(0, event.listenerCount());
    }

    @Test
    public void testListenersRunByPriorityThenRegistrationOrder() {
        Event<Consumer<List<String>>> event = newEvent();
        event.register(log -> log.add("normal-1"));
        event.register(Event.PRIORITY_LOW, log -> log.add("low"));
        event.register(Event.PRIORITY_HIGH, log -> log.add("high"));
        event.register(log -> log.add("normal-2"));

        List<String> log = new ArrayList<>();
        event.invoker().accept(log);
        assertEquals(List.of("high", "normal-1", "normal-2", "low"), log);
    }

    @Test
    public void testUnregister() {
        Event<Consumer<List<String>>> event = newEvent();
        Consumer<List<String>> first = log -> log.add("first");
        Consumer<List<String>> second = log -> log.add("second");
        event.register(first);
        event.register(second);

        assertTrue(event.unregister(first));
        assertFalse(event.unregister(first));
        event.register(log -> log.add("third"));

        List<String> log = new ArrayList<>();
        event.invoker().accept(log);
        assertEquals(List.of("second", "third"), log);
    }

    @Test
    public void testRejectsNullListener() {
        assertThrows(IllegalArgumentException.class, () -> newEvent().register(null));
    }
}
