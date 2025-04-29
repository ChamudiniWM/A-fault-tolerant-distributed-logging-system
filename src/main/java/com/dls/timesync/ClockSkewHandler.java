package com.dls.timesync;

public class ClockSkewHandler {

    // This will store the clock offset detected during synchronization
    private long clockOffsetMillis = 0L;

    /**
     * Sync with NTP server and update internal clock offset.
     */
    public void synchronizeClock() {
        clockOffsetMillis = TimeSynchronizer.getClockOffset();
        System.out.println("[ClockSkewHandler] Updated clock offset: " + clockOffsetMillis + " ms");
    }

    /**
     * Adjust a local timestamp to be "corrected" by clock offset.
     *
     * @param localTimestamp Local timestamp in milliseconds.
     * @return Corrected timestamp in milliseconds.
     */
    public long correctTimestamp(long localTimestamp) {
        return localTimestamp + clockOffsetMillis;
    }

    /**
     * Get current known clock offset.
     *
     * @return offset in milliseconds.
     */
    public long getClockOffsetMillis() {
        return clockOffsetMillis;
    }
}