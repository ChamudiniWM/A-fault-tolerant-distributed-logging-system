package com.group6.logsystem.tests;

import com.group6.logsystem.faulttolerance.redundancy.ErasureCodingHandler;
import com.group6.logsystem.models.InternalLogEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ErasureCodingHandlerTest {
    private ErasureCodingHandler erasureCodingHandler;
    private InternalLogEntry testLogEntry;
    private final String storagePath = "storage/test-node/erasure";

    @BeforeEach
    void setup() {
        // Use the existing single-parameter constructor
        erasureCodingHandler = new ErasureCodingHandler("test-node");
        
        // Create a test log entry
        testLogEntry = new InternalLogEntry(
            UUID.randomUUID().toString(),
            1,
            "node1",
            "Test log content for erasure coding",
            System.currentTimeMillis(),
            1
        );
    }
    
    @AfterEach
    void cleanup() {
        // Clean up the default storage directory
        try {
            File storageDir = new File(storagePath);
            if (storageDir.exists()) {
                File[] files = storageDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        Files.deleteIfExists(file.toPath());
                    }
                }
                Files.deleteIfExists(storageDir.toPath());
            }
            erasureCodingHandler.shutdown();
        } catch (Exception e) {
            System.err.println("Failed to clean up storage directory: " + e.getMessage());
        }
    }

    @Test
    void testEncodeAndStoreLog() throws Exception {
        // Encode and store the log
        CompletableFuture<Boolean> future = erasureCodingHandler.encodeAndStoreLog(testLogEntry);
        Boolean result = future.get(5, TimeUnit.SECONDS);
        
        // Verify the result
        assertTrue(result, "Encoding and storing log should succeed");
        
        // Verify files were created in the default storage path
        File metadataFile = new File(storagePath, testLogEntry.getLogId() + ".meta");
        assertTrue(metadataFile.exists(), "Metadata file should be created");
        
        File dataFile = new File(storagePath, testLogEntry.getLogId() + ".data");
        assertTrue(dataFile.exists(), "Data file should be created");
    }

    @Test
    void testHasLog() throws Exception {
        // First store the log
        erasureCodingHandler.encodeAndStoreLog(testLogEntry).get(5, TimeUnit.SECONDS);
        
        // Check if the log exists
        CompletableFuture<Boolean> future = erasureCodingHandler.hasLog(testLogEntry.getLogId());
        Boolean result = future.get(5, TimeUnit.SECONDS);
        
        // Verify the result
        assertTrue(result, "Should report that the log exists");
        
        // Check for a non-existent log
        future = erasureCodingHandler.hasLog("non-existent-log");
        result = future.get(5, TimeUnit.SECONDS);
        
        // Verify the result
        assertFalse(result, "Should report that the log does not exist");
    }

    @Test
    void testRetrieveLog() throws Exception {
        // First store the log
        erasureCodingHandler.encodeAndStoreLog(testLogEntry).get(5, TimeUnit.SECONDS);
        
        // Retrieve the log
        CompletableFuture<InternalLogEntry> future = erasureCodingHandler.retrieveLog(testLogEntry.getLogId());
        InternalLogEntry result = future.get(5, TimeUnit.SECONDS);
        
        // Verify the result
        assertNotNull(result, "Should retrieve the log");
        assertEquals(testLogEntry.getLogId(), result.getLogId(), "Retrieved log should have correct ID");
        assertEquals(testLogEntry.getMessage(), result.getMessage(), "Retrieved log should have correct message");
    }

    @Test
    void testRetrieveNonExistentLog() {
        // Try to retrieve a non-existent log
        CompletableFuture<InternalLogEntry> future = erasureCodingHandler.retrieveLog("non-existent-log");
        
        // Verify the future completes exceptionally
        Exception exception = assertThrows(Exception.class, () -> future.get(5, TimeUnit.SECONDS));
        assertTrue(exception.getCause().getMessage().contains("not found"), 
                  "Should throw an exception for non-existent log");
    }

    @Test
    void testMultipleLogsStorage() throws Exception {
        // Create multiple test logs
        InternalLogEntry[] testLogs = new InternalLogEntry[5];
        for (int i = 0; i < 5; i++) {
            testLogs[i] = new InternalLogEntry(
                "test-log-" + i,
                i,
                "node1",
                "Test log content " + i,
                System.currentTimeMillis(),
                1
            );
        }
        
        // Store all logs
        for (InternalLogEntry log : testLogs) {
            erasureCodingHandler.encodeAndStoreLog(log).get(5, TimeUnit.SECONDS);
        }
        
        // Verify all logs can be retrieved
        for (InternalLogEntry log : testLogs) {
            InternalLogEntry retrieved = erasureCodingHandler.retrieveLog(log.getLogId()).get(5, TimeUnit.SECONDS);
            assertNotNull(retrieved, "Should retrieve log " + log.getLogId());
            assertEquals(log.getMessage(), retrieved.getMessage(), "Retrieved log should have correct message");
        }
    }
}