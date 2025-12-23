package scheduling;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TiredExecutorTest {

    /**
     * Test 1: Basic Sanity Check
     * Runs a number of tasks and verifies that all were executed and the final value is correct.
     * This verifies that submitAll indeed waits until all tasks are finished.
     */
    @Test
    void testSubmitAllWaitsForCompletion() {
        int numThreads = 4;
        int numTasks = 100;
        TiredExecutor executor = new TiredExecutor(numThreads);

        AtomicInteger counter = new AtomicInteger(0);

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < numTasks; i++) {
            tasks.add(() -> {
                try { Thread.sleep(1); } catch (InterruptedException e) {}
                counter.incrementAndGet();
            });
        }

        // This command will block the current thread until all of the 100 threads done with their tasks
        executor.submitAll(tasks);

        assertEquals(numTasks, counter.get(), "Not all tasks were executed!");

        // cleanup
        try { executor.shutdown(); } catch (InterruptedException e) {}
    }

    /**
     * Test 2: Concurrency Check
     * If we have 4 threads and 4 tasks that take 200ms each,
     * the total time should be around 200ms (parallel) and not 800ms (sequential).
     */
    @Test
    void testParallelExecution() {
        int numThreads = 4;
        TiredExecutor executor = new TiredExecutor(numThreads);

        List<Runnable> tasks = new ArrayList<>();
        int sleepTime = 200;

        // Create 4 tasks, each sleeps for 200ms
        for (int i = 0; i < numThreads; i++) {
            tasks.add(() -> {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        long startTime = System.currentTimeMillis();
        executor.submitAll(tasks);
        long endTime = System.currentTimeMillis();

        long totalTime = endTime - startTime;

        // Expectation: Total time should be significantly less than the sum of times (4 * 200 = 800)
        // We add a safety margin (e.g., it should take less than 640ms)
        assertTrue(totalTime < (sleepTime * numThreads * 0.8),
                "Tasks ran sequentially instead of in parallel! Took: " + totalTime + "ms");

        try { executor.shutdown(); } catch (InterruptedException e) {}
    }

    /**
     * Test 3: Exception Resilience
     * Verifies that if a task throws an exception, the Executor does not crash
     * and correctly decrements the inFlight counter so submitAll doesn't hang forever.
     */
    @Test
    void testExceptionHandling() {
        int numThreads = 2;
        TiredExecutor executor = new TiredExecutor(numThreads);

        List<Runnable> tasks = new ArrayList<>();

        // Task 1: Throws an exception
        tasks.add(() -> {
            throw new RuntimeException("I crashed!");
        });

        // Task 2: Normal task
        tasks.add(() -> {
            try { Thread.sleep(10); } catch (InterruptedException e) {}
        });

        // This line will hang forever if the exception breaks the inFlight logic
        try {
            executor.submitAll(tasks);
        } catch (Exception e) {
            fail("Executor threw exception instead of handling it gracefully");
        }

        // If we reached here, it means the executor recovered and finished!
        assertTrue(true);

        try { executor.shutdown(); } catch (InterruptedException e) {}
    }

    /**
     * Test 4: Work Distribution (Simple Version)
     * בודק שכל העובדים משתתפים במאמץ.
     * במקום להשתמש ב-Set, אנחנו משתמשים במערך מונים לפי ה-ID של העובד.
     */
    @Test
    void testWorkDistributionSimple() {
        int numThreads = 4;
        int numTasks = 100;
        TiredExecutor executor = new TiredExecutor(numThreads);

        // מערך מונים: תא 0 עבור עובד 0, תא 1 עבור עובד 1 וכו'
        AtomicInteger[] workerCounts = new AtomicInteger[numThreads];

        // אתחול המערך
        for (int i = 0; i < numThreads; i++) {
            workerCounts[i] = new AtomicInteger(0);
        }

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < numTasks; i++) {
            tasks.add(() -> {
                // אנחנו משיגים את הת'רד הנוכחי שמריץ את המשימה הזו
                Thread currentThread = Thread.currentThread();

                // בודקים אם זה אכן העובד שלנו (TiredThread)
                if (currentThread instanceof TiredThread) {
                    // המרה (Cast) כדי שנוכל לגשת ל-ID שלו
                    TiredThread worker = (TiredThread) currentThread;
                    int id = worker.getWorkerId();

                    // העלאת המונה של העובד הספציפי הזה
                    workerCounts[id].incrementAndGet();
                }
            });
        }

        executor.submitAll(tasks);

        // שלב הבדיקה: עוברים על המערך ומוודאים שכל עובד עשה לפחות משימה אחת
        for (int i = 0; i < numThreads; i++) {
            int count = workerCounts[i].get();
            System.out.println("Worker " + i + " completed " + count + " tasks.");

            assertTrue(count > 0, "Worker " + i + " was lazy and did nothing!");
        }

        try { executor.shutdown(); } catch (InterruptedException e) {}
    }


    /**
     * Test 4: Work Distribution (Fairness Logic)
     * Verifies that the PriorityBlockingQueue correctly balances the load.
     * Since we base scheduling on "Fatigue", the work should be distributed roughly evenly.
     * We ensure that no worker is starved and that everyone performs a significant chunk of work.
     */
    @Test
    void testWorkDistribution() {
        int numThreads = 4;
        int numTasks = 100;

        // Expected average is 100 / 4 = 25 tasks per worker.
        // We set a safe minimum threshold (e.g., 5 tasks).
        // If the logic is correct, everyone should easily pass this threshold.
        int minExpectedTasks = 5;

        TiredExecutor executor = new TiredExecutor(numThreads);

        // Array to count tasks performed by each worker (Index = Worker ID)
        AtomicInteger[] workerCounts = new AtomicInteger[numThreads];

        // Initialize counters
        for (int i = 0; i < numThreads; i++) {
            workerCounts[i] = new AtomicInteger(0);
        }

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < numTasks; i++) {
            tasks.add(() -> {
                // Get current thread
                Thread currentThread = Thread.currentThread();

                // Verify it is one of our workers and increment its specific counter
                if (currentThread instanceof TiredThread) {
                    TiredThread worker = (TiredThread) currentThread;
                    int id = worker.getWorkerId();
                    workerCounts[id].incrementAndGet();
                }
            });
        }

        // Run all tasks
        executor.submitAll(tasks);

        // Assertions: Validate distribution
        for (int i = 0; i < numThreads; i++) {
            int count = workerCounts[i].get();
            System.out.println("Worker " + i + " completed " + count + " tasks.");

            // Ensure the worker did a reasonable amount of work
            assertTrue(count >= minExpectedTasks,
                    "Worker " + i + " is under-utilized! Only did " + count + " tasks (Expected at least " + minExpectedTasks + ")");
        }

        try { executor.shutdown(); } catch (InterruptedException e) {}
    }
}