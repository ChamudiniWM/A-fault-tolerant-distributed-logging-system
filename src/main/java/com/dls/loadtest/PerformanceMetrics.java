package com.dls.loadtest;

import java.util.concurrent.atomic.AtomicLong;

public class PerformanceMetrics {

    private static final AtomicLong totalCommits = new AtomicLong(0);
    private static final AtomicLong totalLatencyMicros = new AtomicLong(0);

    public static void recordCommitLatency(long latencyMicros) {
        totalCommits.incrementAndGet();
        totalLatencyMicros.addAndGet(latencyMicros);
    }

    public static void printMetrics() {
        long commits = totalCommits.get();
        long avgLatency = commits == 0 ? 0 : totalLatencyMicros.get() / commits;
        System.out.println("========== Performance Metrics ==========");
        System.out.println("Total Commits: " + commits);
        System.out.println("Average Commit Latency: " + avgLatency + " µs");
    }
}