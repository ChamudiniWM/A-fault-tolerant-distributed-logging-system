package com.group6.logsystem.timesync;

import java.time.Instant;

public class TimeSyncManager {

    private static final String NTP_SERVER = "time.google.com"; // Placeholder

    public static long getNetworkTimeMillis() {
        try {
            // In actual implementation, use an NTP client or trusted source
            return Instant.now().toEpochMilli();  // simulate network time
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    public static long calculateClockOffset() {
        long localTime = System.currentTimeMillis();
        long networkTime = getNetworkTimeMillis();
        return networkTime - localTime;
    }

    public static long getCorrectedTimestamp() {
        return System.currentTimeMillis() + calculateClockOffset();
    }
}
