package client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RaftClient {

    private static final String DEFAULT_HOST = "localhost";
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter leader node port to connect to: ");
        int port;

        try {
            port = Integer.parseInt(scanner.nextLine());
        } catch (Exception e) {
            System.err.println("Invalid port. Exiting.");
            return;
        }

        ManagedChannel channel = ManagedChannelBuilder.forAddress(DEFAULT_HOST, port)
                .usePlaintext()
                .build();
        ClientServiceGrpc.ClientServiceBlockingStub stub = ClientServiceGrpc.newBlockingStub(channel);

        try {
            client.Client.IsLeaderResponse response = stub.isLeader(client.Client.Empty.newBuilder().build());

            if (!response.getIsLeader()) {
                System.out.println("Connected to follower. Redirecting to leader...");
                String leaderHost = response.getLeaderHost();
                int leaderPort = response.getLeaderPort();
                System.out.printf("Leader is at %s:%d\n", leaderHost, leaderPort);

                channel.shutdownNow();
                channel = ManagedChannelBuilder.forAddress(leaderHost, leaderPort)
                        .usePlaintext()
                        .build();
                stub = ClientServiceGrpc.newBlockingStub(channel);
            } else {
                System.out.println("Connected to leader.");
            }

            runClientLoop(scanner, stub, channel);

        } catch (StatusRuntimeException e) {
            System.err.println("Failed to determine leader: " + e.getMessage());
        } finally {
            channel.shutdownNow();
        }
    }

    private static void runClientLoop(Scanner scanner, ClientServiceGrpc.ClientServiceBlockingStub stub, ManagedChannel channel) {
        System.out.println("Enter 'send' to send log, 'query' to retrieve logs, 'stress' to run stress test, or 'exit':");

        while (true) {
            System.out.print(">> ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Exiting client.");
                break;
            } else if (input.equalsIgnoreCase("send")) {
                System.out.print("Enter log: ");
                String command = scanner.nextLine();
                stub = sendCommandWithRedirect(stub, command, channel);
            } else if (input.equalsIgnoreCase("query")) {
                handleLogQuery(scanner, stub);
            } else if (input.equalsIgnoreCase("stress")) {
                stub = runStressTest(scanner, stub, channel);
            } else {
                System.out.println("Unknown command. Type 'send', 'query', 'stress', or 'exit'.");
            }
        }
    }

    private static ClientServiceGrpc.ClientServiceBlockingStub sendCommandWithRedirect(ClientServiceGrpc.ClientServiceBlockingStub stub, String command, ManagedChannel channel) {
        try {
            client.Client.ClientCommandRequest request = client.Client.ClientCommandRequest.newBuilder()
                    .setCommand(command)
                    .build();

            client.Client.ClientCommandResponse response = stub.sendCommand(request);

            if (response.getSuccess()) {
                System.out.println("Command sent successfully: " + response.getMessage());
                return stub;
            } else if (!response.getLeaderHost().isEmpty() && response.getLeaderPort() != 0) {
                System.out.printf("Redirected to leader at %s:%d\n", response.getLeaderHost(), response.getLeaderPort());

                // Close the current channel and connect to the new leader
                channel.shutdownNow();
                ManagedChannel newChannel = ManagedChannelBuilder.forAddress(response.getLeaderHost(), response.getLeaderPort())
                        .usePlaintext()
                        .build();
                ClientServiceGrpc.ClientServiceBlockingStub newStub = ClientServiceGrpc.newBlockingStub(newChannel);

                // Retry command after redirection
                client.Client.ClientCommandResponse retryResponse = newStub.sendCommand(request);
                if (retryResponse.getSuccess()) {
                    System.out.println("Command retried successfully: " + retryResponse.getMessage());
                } else {
                    System.out.println("Retry failed: " + retryResponse.getMessage());
                }
                return newStub; // Return the new stub connected to the leader
            } else {
                System.out.println("Failed to send command: " + response.getMessage());
                return stub;
            }
        } catch (StatusRuntimeException e) {
            System.err.println("Error sending command: " + e.getMessage());
            return stub;
        }
    }

    private static ClientServiceGrpc.ClientServiceBlockingStub runStressTest(Scanner scanner, ClientServiceGrpc.ClientServiceBlockingStub stub, ManagedChannel channel) {
        try {
            System.out.print("Enter number of commands to send (e.g., 10000): ");
            int numCommands = Integer.parseInt(scanner.nextLine());

            System.out.println(SDF.format(new Date()) + " Starting stress test...");
            long startTime = System.nanoTime();
            long totalLatency = 0;
            long commandCount = 0;

            // Send commands in a loop using a single client stub
            for (int i = 0; i < numCommands; i++) {
                try {
                    long taskStartTime = System.nanoTime();
                    String command = "stress-command-" + i;
                    long serverTime = stub.getServerTime(client.Client.Empty.newBuilder().build()).getTimestamp();

                    client.Client.ClientCommandRequest request = client.Client.ClientCommandRequest.newBuilder()
                            .setCommand(command)
                            .build();

                    client.Client.ClientCommandResponse response = stub.sendCommand(request);
                    long latencyMs = (System.nanoTime() - taskStartTime) / 1_000_000;

                    if (response.getSuccess()) {
                        totalLatency += latencyMs;
                        commandCount++;
                        System.out.println(SDF.format(new Date()) + " Command " + i + " sent successfully, latency: " + latencyMs + "ms");
                    } else if (!response.getLeaderHost().isEmpty() && response.getLeaderPort() != 0) {
                        // Handle leader redirection
                        System.out.println(SDF.format(new Date()) + " Redirecting to leader at " + response.getLeaderHost() + ":" + response.getLeaderPort());
                        channel.shutdownNow();
                        ManagedChannel newChannel = ManagedChannelBuilder.forAddress(response.getLeaderHost(), response.getLeaderPort())
                                .usePlaintext()
                                .build();
                        stub = ClientServiceGrpc.newBlockingStub(newChannel);
                        // Retry command
                        response = stub.sendCommand(request);
                        if (response.getSuccess()) {
                            totalLatency += latencyMs;
                            commandCount++;
                            System.out.println(SDF.format(new Date()) + " Command " + i + " retried successfully, latency: " + latencyMs + "ms");
                        } else {
                            System.err.println(SDF.format(new Date()) + " Command " + i + " retry failed: " + response.getMessage());
                        }
                    } else {
                        System.err.println(SDF.format(new Date()) + " Command " + i + " failed: " + response.getMessage());
                    }
                } catch (Exception e) {
                    System.err.println(SDF.format(new Date()) + " Error sending command " + i + ": " + e.getMessage());
                }
            }

            long endTime = System.nanoTime();
            double durationSeconds = (endTime - startTime) / 1_000_000_000.0;
            double throughput = commandCount / durationSeconds;
            double avgLatencyMs = commandCount > 0 ? totalLatency / (double) commandCount : 0;

            // Print metrics
            System.out.println(SDF.format(new Date()) + " Stress test completed");
            System.out.println("Total commands sent: " + commandCount);
            System.out.println("Duration (seconds): " + durationSeconds);
            System.out.println("Throughput (commands/second): " + throughput);
            System.out.println("Average latency per command (ms): " + avgLatencyMs);

            // Query logs to verify consistency
            long startTimestamp = System.currentTimeMillis() - 60_000;
            long endTimestamp = System.currentTimeMillis();
            client.Client.LogQueryRequest query = client.Client.LogQueryRequest.newBuilder()
                    .setStartTimestamp(startTimestamp)
                    .setEndTimestamp(endTimestamp)
                    .setAscending(true)
                    .build();
            client.Client.LogQueryResponse response = stub.queryLogs(query);
            System.out.println(SDF.format(new Date()) + " Queried " + response.getEntriesCount() + " log entries");
            for (client.Client.LogEntryData entry : response.getEntriesList()) {
                Instant instant = Instant.ofEpochMilli(entry.getTimestamp());
                ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
                System.out.printf("  - [%s] ID=%s Command=%s\n",
                        zdt.toLocalTime().truncatedTo(ChronoUnit.SECONDS),
                        entry.getLogId(),
                        entry.getCommand());
            }

            return stub;
        } catch (Exception e) {
            System.err.println("Error running stress test: " + e.getMessage());
            return stub;
        }
    }

    private static void handleLogQuery(Scanner scanner, ClientServiceGrpc.ClientServiceBlockingStub stub) {
        try {
            System.out.print("Enter start time (HHmm, e.g., 1300): ");
            String start = scanner.nextLine();
            System.out.print("Enter end time (HHmm, e.g., 1330): ");
            String end = scanner.nextLine();
            System.out.print("Sort ascending? (yes/no): ");
            String order = scanner.nextLine();

            long startMillis = parseToMillis(start);
            long endMillis = parseToMillis(end);
            boolean ascending = order.equalsIgnoreCase("yes");

            client.Client.LogQueryRequest query = client.Client.LogQueryRequest.newBuilder()
                    .setStartTimestamp(startMillis)
                    .setEndTimestamp(endMillis)
                    .setAscending(ascending)
                    .build();

            client.Client.LogQueryResponse response = stub.queryLogs(query);
            System.out.println("Matching log entries:");
            for (client.Client.LogEntryData entry : response.getEntriesList()) {
                Instant instant = Instant.ofEpochMilli(entry.getTimestamp());
                ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
                System.out.printf("  - [%s] ID=%s Command=%s\n",
                        zdt.toLocalTime().truncatedTo(ChronoUnit.SECONDS),
                        entry.getLogId(),
                        entry.getCommand());
            }
            System.out.println("Please note that logs ordered in the timestamp order not the appended order");
        } catch (Exception e) {
            System.err.println("Error retrieving logs: " + e.getMessage());
        }
    }

    private static long parseToMillis(String hhmm) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HHmm");
        LocalTime time = LocalTime.parse(hhmm, formatter);
        return time.atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}