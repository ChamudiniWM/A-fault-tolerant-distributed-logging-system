package com.dls.common;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.Timer;
import java.util.TimerTask;

public class RejectionTracker {

    private static final AtomicInteger rejectionCount = new AtomicInteger(0);

    public static void increment() {
        rejectionCount.incrementAndGet();
    }

    public static void startLogging() {
        Timer timer = new Timer(true); // Daemon timer
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                int count = rejectionCount.getAndSet(0);
                if (count > 0) {
                    System.out.println("[RejectionTracker] " + count + " AppendEntries rejected in the last second.");
                }
            }
        }, 1000, 1000); // Every 1 second
    }
}