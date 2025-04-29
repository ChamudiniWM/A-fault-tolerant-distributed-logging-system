package com.dls.timesync;

public class TimestampCorrector {

    private final ClockSkewHandler clockSkewHandler;

    public TimestampCorrector(ClockSkewHandler clockSkewHandler) {
        this.clockSkewHandler = clockSkewHandler;
    }

    /**
     * Corrects the given timestamp using the known clock offset.
     *
     * @param originalTimestamp The original timestamp (local time).
     * @return Corrected timestamp after applying clock offset.
     */
    public long correct(long originalTimestamp) {
        return clockSkewHandler.correctTimestamp(originalTimestamp);
    }
}