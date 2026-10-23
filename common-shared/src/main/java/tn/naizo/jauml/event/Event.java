package tn.naizo.jauml.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * A loader-neutral event: listeners register once, and the loader module calls {@link #invoker()} when it happens.
 * Listeners run in priority order (higher first), then registration order.
 * <pre>{@code
 * public static final Event<Runnable> RELOADED = Event.create(listeners -> () -> listeners.forEach(Runnable::run));
 * RELOADED.register(() -> ...);
 * RELOADED.invoker().run();
 * }</pre>
 *
 * @param <T> the listener type, usually a functional interface
 * @since 3.0.0
 */
public final class Event<T> {

    public static final int PRIORITY_HIGH = 100;
    public static final int PRIORITY_NORMAL = 0;
    public static final int PRIORITY_LOW = -100;

    private final Function<List<T>, T> invokerFactory;
    private final List<Entry<T>> entries = new ArrayList<>();
    private int nextOrder;
    private volatile T invoker;

    private Event(Function<List<T>, T> invokerFactory) {
        this.invokerFactory = invokerFactory;
        this.invoker = invokerFactory.apply(Collections.emptyList());
    }

    /**
     * Creates an event. The factory turns the current listener list into one listener that calls them all;
     * it runs again whenever a listener is added or removed.
     */
    public static <T> Event<T> create(Function<List<T>, T> invokerFactory) {
        return new Event<>(invokerFactory);
    }

    public void register(T listener) {
        register(PRIORITY_NORMAL, listener);
    }

    public synchronized void register(int priority, T listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        entries.add(new Entry<>(priority, nextOrder++, listener));
        rebuild();
    }

    /**
     * Removes a listener registered earlier.
     *
     * @return true if the listener was registered
     */
    public synchronized boolean unregister(T listener) {
        boolean removed = entries.removeIf(entry -> entry.listener == listener);
        if (removed) {
            rebuild();
        }
        return removed;
    }

    public synchronized int listenerCount() {
        return entries.size();
    }

    /**
     * Returns the listener that calls every registered listener. Loader modules call this to fire the event.
     */
    public T invoker() {
        return invoker;
    }

    private void rebuild() {
        List<Entry<T>> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.<Entry<T>>comparingInt(entry -> -entry.priority).thenComparingInt(entry -> entry.order));
        List<T> listeners = new ArrayList<>(sorted.size());
        for (Entry<T> entry : sorted) {
            listeners.add(entry.listener);
        }
        invoker = invokerFactory.apply(Collections.unmodifiableList(listeners));
    }

    private static final class Entry<T> {
        final int priority;
        final int order;
        final T listener;

        Entry(int priority, int order, T listener) {
            this.priority = priority;
            this.order = order;
            this.listener = listener;
        }
    }
}
