package com.dls.timesync;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClockSkewHandler {

    // Each node can have its own offset (e.g., node1 is -30ms, node2 is +50ms)
    private final Map<String, Long> nodeClockOffsets = new ConcurrentHashMap<>();

    /**
     * Set the clock offset for a specific node.
     *
     * @param nodeId Node ID (e.g., "node1")
     * @param offset Clock offset in milliseconds
     */
    public void setClockOffset(String nodeId, long offset) {
        nodeClockOffsets.put(nodeId, offset);
    }

    /**
     * Get the clock offset for a specific node.
     *
     * @param nodeId Node ID
     * @return offset in milliseconds
     */
    public long getClockOffset(String nodeId) {
        return nodeClockOffsets.getOrDefault(nodeId, 0L); // Assume 0 offset if unknown
    }

    /**
     * Correct the timestamp received from a node.
     *
     * @param nodeId Node ID
     * @param timestamp Timestamp from that node
     * @return Corrected timestamp
     */
    public long correctTimestamp(String nodeId, long timestamp) {
        return timestamp + getClockOffset(nodeId);
    }
}