package com.dls.common;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;

public class PerformanceLogger {

    private static final String FILE_NAME = "performance_log.csv";
    private static boolean headerWritten = false;

    public static synchronized void logLatency(long latencyMicros) {
        try (FileWriter writer = new FileWriter(FILE_NAME, true)) {
            if (!headerWritten) {
                writer.write("timestamp,latency_micros\n");
                headerWritten = true;
            }
            String timestamp = Instant.now().toString();
            writer.write(timestamp + "," + latencyMicros + "\n");
        } catch (IOException e) {
            System.err.println("[PerformanceLogger] Failed to write latency: " + e.getMessage());
        }
    }
}