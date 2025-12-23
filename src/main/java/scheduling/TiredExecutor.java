package scheduling;

import java.util.Random;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredExecutor {

    private final TiredThread[] workers;
    private final PriorityBlockingQueue<TiredThread> idleMinHeap = new PriorityBlockingQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);

    public TiredExecutor(int numThreads) {
        // TODO
        workers = new TiredThread[numThreads];// placeholder
        for(int i=0;i<numThreads;i++){
            double fatigue = Math.random() + 0.5;
            TiredThread thread = new TiredThread(i, fatigue);
            workers[i] = thread;
            idleMinHeap.add(thread);
            thread.start();
        }

    }

    public void submit(Runnable task) {
        // TODO
        try {
            inFlight.incrementAndGet();
            TiredThread threadToSubmit = idleMinHeap.take();
            Runnable wrappedTask = () -> {
                try{
                    task.run();
                }
                finally{
                    inFlight.decrementAndGet();
                    idleMinHeap.add(threadToSubmit);
                    if(inFlight.get() ==0) {
                        synchronized (inFlight) {
                            inFlight.notifyAll();
                        }
                    }
                }
            };
            threadToSubmit.newTask(wrappedTask);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }

    public void submitAll(Iterable<Runnable> tasks) {
        for (Runnable task : tasks) {
            submit(task);
        }
        synchronized (inFlight) {
            while (inFlight.get() > 0) { // Check the actual atomic counter
                try {
                    inFlight.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void shutdown() throws InterruptedException {
        // TODO
        for(TiredThread worker: workers){
            worker.shutdown();
            if (Thread.interrupted())
                throw new InterruptedException();
        }
    }

    public synchronized String getWorkerReport() {
        // TODO: return readable statistics for each worker
        StringBuilder reports = new StringBuilder();

        for (TiredThread worker : workers) {
            // Converting nano seconds into milli seconds
            long usedMs = worker.getTimeUsed() / 1_000_000;
            long idleMs = worker.getTimeIdle() / 1_000_000;

            String report = "Worker " + worker.getWorkerId() +
                    ":\tFatigue: " + worker.getFatigue() +
                    "\tTime used: " + usedMs + " ms" +
                    "\tTime idle: " + idleMs + " ms";

            reports.append(report).append("\n");
        }

        reports.append("----------------------------------------\n");
        reports.append("Fairness Score (Lower is better): ")
                .append(String.format("%.4f", getFairnessScore()));

        return reports.toString();
    }

    private double getFairnessScore() {
        // Calculate fatigue average
        double totalFatigue = 0;
        for (TiredThread worker : workers) {
            totalFatigue += worker.getFatigue();
        }
        double averageFatigue = totalFatigue / workers.length;

        // Calculating the time of squared deviations from the average
        double sumSquaredDeviations = 0;
        for (TiredThread worker : workers) {
            double deviation = worker.getFatigue() - averageFatigue;
            sumSquaredDeviations += (deviation * deviation);
        }

        return sumSquaredDeviations;
    }


}
