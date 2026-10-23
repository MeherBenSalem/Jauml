package tn.naizo.jauml.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Runs work on the server thread after a number of ticks, or repeatedly. Tasks may be scheduled from any thread;
 * they always run on the server thread at the end of a tick. Pending tasks are dropped when the server stops.
 * <pre>{@code
 * Scheduler.runLater(20, () -> player.sendSystemMessage(...));         // in one second
 * Scheduler.Task pulse = Scheduler.runRepeating(0, 100, MyMod::pulse); // every five seconds
 * pulse.cancel();
 * }</pre>
 *
 * @since 3.0.0
 */
public final class Scheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger("JaumlScheduler");

    private static final Queue<ScheduledTask> INCOMING = new ConcurrentLinkedQueue<>();
    private static final List<ScheduledTask> ACTIVE = new ArrayList<>();
    private static volatile long currentTick;

    private Scheduler() {}

    /**
     * Runs a task at the end of the next server tick.
     */
    public static Task runNextTick(Runnable action) {
        return runLater(1, action);
    }

    /**
     * Runs a task once after the given number of ticks (20 ticks is one second at normal speed).
     */
    public static Task runLater(int delayTicks, Runnable action) {
        return schedule(Math.max(1, delayTicks), 0, action);
    }

    /**
     * Same as {@link #runLater(int, Runnable)}, named after the MCreator helper so existing
     * {@code queueServerWork(ticks, action)} calls can point here instead of a per-mod work queue.
     */
    public static Task queueServerWork(int delayTicks, Runnable action) {
        return runLater(delayTicks, action);
    }

    /**
     * Runs a task every {@code periodTicks} ticks, starting after {@code delayTicks}, until it is cancelled
     * or the server stops.
     */
    public static Task runRepeating(int delayTicks, int periodTicks, Runnable action) {
        if (periodTicks < 1) {
            throw new IllegalArgumentException("periodTicks must be at least 1");
        }
        return schedule(Math.max(1, delayTicks), periodTicks, action);
    }

    /**
     * Returns how many server ticks the scheduler has seen since the server started.
     */
    public static long currentTick() {
        return currentTick;
    }

    /**
     * Returns how many tasks are waiting to run.
     */
    public static int pendingTasks() {
        synchronized (ACTIVE) {
            int count = 0;
            for (ScheduledTask task : ACTIVE) {
                if (!task.cancelled) {
                    count++;
                }
            }
            for (ScheduledTask task : INCOMING) {
                if (!task.cancelled) {
                    count++;
                }
            }
            return count;
        }
    }

    private static Task schedule(int delayTicks, int periodTicks, Runnable action) {
        if (action == null) {
            throw new IllegalArgumentException("Task action cannot be null");
        }
        ScheduledTask task = new ScheduledTask(action, currentTick + delayTicks, periodTicks);
        INCOMING.add(task);
        return task;
    }

    /**
     * Advances the scheduler by one tick and runs due tasks. Called by {@link ServerEvents.Hooks#tickEnd}.
     */
    static void tick() {
        long now = ++currentTick;
        List<ScheduledTask> due = new ArrayList<>();
        synchronized (ACTIVE) {
            ScheduledTask incoming;
            while ((incoming = INCOMING.poll()) != null) {
                ACTIVE.add(incoming);
            }
            Iterator<ScheduledTask> iterator = ACTIVE.iterator();
            while (iterator.hasNext()) {
                ScheduledTask task = iterator.next();
                if (task.cancelled) {
                    iterator.remove();
                } else if (task.nextRunTick <= now) {
                    due.add(task);
                    if (task.periodTicks > 0) {
                        task.nextRunTick = now + task.periodTicks;
                    } else {
                        iterator.remove();
                    }
                }
            }
        }
        for (ScheduledTask task : due) {
            if (task.cancelled) {
                continue;
            }
            try {
                task.action.run();
            } catch (RuntimeException e) {
                LOGGER.error("Scheduled task {} failed", task.action.getClass().getName(), e);
            }
            if (task.periodTicks == 0) {
                task.done = true;
            }
        }
    }

    /**
     * Drops every pending task and resets the tick counter. Called when the server stops.
     */
    static void clear() {
        synchronized (ACTIVE) {
            for (ScheduledTask task : ACTIVE) {
                task.cancelled = true;
            }
            ScheduledTask incoming;
            while ((incoming = INCOMING.poll()) != null) {
                incoming.cancelled = true;
            }
            ACTIVE.clear();
            currentTick = 0;
        }
    }

    /**
     * A handle to a scheduled task.
     */
    public interface Task {
        void cancel();

        boolean isCancelled();

        /**
         * True once a one-shot task has run. Repeating tasks are never done; cancel them instead.
         */
        boolean isDone();
    }

    private static final class ScheduledTask implements Task {
        final Runnable action;
        final int periodTicks;
        long nextRunTick;
        volatile boolean cancelled;
        volatile boolean done;

        ScheduledTask(Runnable action, long nextRunTick, int periodTicks) {
            this.action = action;
            this.nextRunTick = nextRunTick;
            this.periodTicks = periodTicks;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return done;
        }
    }
}
