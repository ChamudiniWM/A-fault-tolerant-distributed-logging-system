package com.group6.logsystem.faulttolerance.redundancy;

import com.group6.logsystem.models.InternalLogEntry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ErasureCodingHandler {
    private final String nodeId;
    private final String storagePath;
    private final ExecutorService executor;
    private final int dataShards;
    private final int parityShards;

    public ErasureCodingHandler(String nodeId) {
        this.nodeId = nodeId;
        this.storagePath = "storage/" + nodeId + "/erasure";
        this.executor = Executors.newFixedThreadPool(2);
        this.dataShards = 6;
        this.parityShards = 3;

        try {
            Files.createDirectories(Paths.get(storagePath));
        } catch (IOException e) {
            System.err.println("Failed to create storage directory: " + e.getMessage());
        }
    }

    public CompletableFuture<Boolean> encodeAndStoreLog(InternalLogEntry entry) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] serializedLog = serializeLogEntry(entry);

                File dataFile = new File(storagePath, entry.getLogId() + ".data");
                try (FileOutputStream fos = new FileOutputStream(dataFile)) {
                    fos.write(serializedLog);
                }

                File metadataFile = new File(storagePath, entry.getLogId() + ".meta");
                try (FileOutputStream fos = new FileOutputStream(metadataFile)) {
                    String metadata = "dataShards=" + dataShards + "\n" +
                                     "parityShards=" + parityShards + "\n" +
                                     "timestamp=" + System.currentTimeMillis();
                    fos.write(metadata.getBytes());
                }

                return true;
            } catch (Exception e) {
                System.err.println("Error encoding and storing log: " + e.getMessage());
                return false;
            }
        }, executor);
    }

    public CompletableFuture<Boolean> hasLog(String logId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File metadataFile = new File(storagePath, logId + ".meta");
                return metadataFile.exists();
            } catch (Exception e) {
                System.err.println("Error checking log existence: " + e.getMessage());
                return false;
            }
        }, executor);
    }

    public CompletableFuture<InternalLogEntry> retrieveLog(String logId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File metadataFile = new File(storagePath, logId + ".meta");
                if (!metadataFile.exists()) {
                    throw new IOException("Log metadata not found: " + logId);
                }

                File dataFile = new File(storagePath, logId + ".data");
                if (!dataFile.exists()) {
                    throw new IOException("Log data not found: " + logId);
                }

                byte[] data = Files.readAllBytes(dataFile.toPath());
                return deserializeLogEntry(data);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    private byte[] serializeLogEntry(InternalLogEntry entry) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(entry);
            return baos.toByteArray();
        }
    }

    private InternalLogEntry deserializeLogEntry(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (InternalLogEntry) ois.readObject();
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}