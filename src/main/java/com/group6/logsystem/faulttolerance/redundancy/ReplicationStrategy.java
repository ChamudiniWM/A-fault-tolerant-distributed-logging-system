package com.group6.logsystem.faulttolerance.redundancy;

/**
 * Enum representing different replication strategies
 */
public enum ReplicationStrategy {
    FULL_REPLICATION,  // Full copies of logs on multiple nodes
    ERASURE_CODING,    // Erasure coding for space efficiency
    HYBRID            // Combination of full replication and erasure coding
}