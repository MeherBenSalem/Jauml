package tn.naizo.jauml.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class SchedulerTest {

    @BeforeEach
    @AfterEach
    public void reset() {
        Scheduler.clear();
    }

    private static void tick(int times) {
        for (int i = 0; i < times; i++) {
            Scheduler.tick();
        }
    }

    @Test
    public void testRunLaterRunsOnceAfterDelay() {
        AtomicInteger runs = new AtomicInteger();
        Scheduler.Task task = Scheduler.runLater(3, runs::incrementAndGet);

        tick(2);
        assertEquals(0, runs.get());
        assertFalse(task.isDone());

        tick(1);
        assertEquals(1, runs.get());
        assertTrue(task.isDone());

        tick(10);
        assertEquals(1, runs.get());
        assertEquals(0, Scheduler.pendingTasks());
    }

    @Test
    public void testRunNextTick() {
        AtomicInteger runs = new AtomicInteger();
        Scheduler.runNextTick(runs::incrementAndGet);
        tick(1);
        assertEquals(1, runs.get());
    }

    @Test
    public void testRepeatingTaskRunsUntilCancelled() {
        AtomicInteger runs = new AtomicInteger();
        Scheduler.Task task = Scheduler.runRepeating(1, 2, runs::incrementAndGet);

        tick(1);
        assertEquals(1, runs.get());
        tick(4);
        assertEquals(3, runs.get());
        assertFalse(task.isDone());

        task.cancel();
        assertTrue(task.isCancelled());
        tick(10);
        assertEquals(3, runs.get());
        assertEquals(0, Scheduler.pendingTasks());
    }

    @Test
    public void testTaskScheduledDuringTickRunsLater() {
        List<String> log = new ArrayList<>();
        Scheduler.runNextTick(() -> {
            log.add("outer");
            Scheduler.runNextTick(() -> log.add("inner"));
        });

        tick(1);
        assertEquals(List.of("outer"), log);
        tick(1);
        assertEquals(List.of("outer", "inner"), log);
    }

    @Test
    public void testFailingTaskDoesNotStopOthers() {
        AtomicInteger runs = new AtomicInteger();
        Scheduler.runNextTick(() -> {
            throw new IllegalStateException("boom");
        });
        Scheduler.runNextTick(runs::incrementAndGet);

        assertDoesNotThrow(() -> tick(1));
        assertEquals(1, runs.get());
    }

    @Test
    public void testClearDropsPendingTasksAndResetsTicks() {
        AtomicInteger runs = new AtomicInteger();
        Scheduler.Task task = Scheduler.runLater(5, runs::incrementAndGet);
        tick(2);
        assertEquals(2, Scheduler.currentTick());

        Scheduler.clear();
        assertTrue(task.isCancelled());
        assertEquals(0, Scheduler.currentTick());
        tick(10);
        assertEquals(0, runs.get());
    }

    @Test
    public void testQueueServerWorkMatchesRunLater() {
        AtomicInteger runs = new AtomicInteger();
        Scheduler.queueServerWork(2, runs::incrementAndGet);
        tick(1);
        assertEquals(0, runs.get());
        tick(1);
        assertEquals(1, runs.get());
    }

    // ServerEvents.Hooks is not tested here: its methods take MinecraftServer, which the 1.20.1 test classpath lacks.

    @Test
    public void testInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> Scheduler.runRepeating(0, 0, () -> {}));
        assertThrows(IllegalArgumentException.class, () -> Scheduler.runLater(1, null));
    }
}
