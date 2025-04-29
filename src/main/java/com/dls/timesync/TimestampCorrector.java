package com.dls.timesync;

public class TimestampCorrector {

    private final ClockSkewHandler clockSkewHandler;

    public TimestampCorrector(ClockSkewHandler clockSkewHandler) {
        this.clockSkewHandler = clockSkewHandler;
    }

    /**
     * Corrects the timestamp from a specific node using its known clock skew.
     *
     * @param nodeId            ID of the node that generated the timestamp.
     * @param originalTimestamp The original timestamp from that node.
     * @return Corrected timestamp.
     */
    public long correct(String nodeId, long originalTimestamp) {
        return clockSkewHandler.correctTimestamp(nodeId, originalTimestamp);
    }
}
