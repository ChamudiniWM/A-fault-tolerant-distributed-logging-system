# Distributed Logging System

## Project Title

**Designing a Fault-Tolerant Distributed Logging System**

## Project Description

A fault-tolerant, time-synchronized, and replicated distributed logging platform built as part of our Distributed Systems group project. The system collects, stores, and processes logs from multiple clients, ensuring high availability, fault tolerance, and consistency across distributed servers with real-time log storage, indexing, and querying.

## Team Members

### Nikini Bandara
- **Registration Number:** IT23148840
- **Email:** [it23148840@my.sliit.lk](mailto:it23148840@my.sliit.lk)
- **Role:** Fault Tolerance & Failure Detection

### Sithija Oshan
- **Registration Number:** IT23281950
- **Email:** [it23281950@my.sliit.lk](mailto:it23281950@my.sliit.lk)
- **Role:** Replication & Consistency

### Madara Chamudini
- **Registration Number:** IT23292154
- **Email:** [it23292154@my.sliit.lk](mailto:it23292154@my.sliit.lk)
- **Role:** Time Synchronization

### Pavan Kumarage
- **Registration Number:** IT23178540
- **Email:** [it23178540@my.sliit.lk](mailto:it23178540@my.sliit.lk)
- **Role:** Consensus (Raft)

## Tech Stack

- **Language:** Java 23
- **Communication:** gRPC + Protobuf
- **Build Tool:** Maven
- **Consensus:** Raft
- **Time Synchronization:** NTP-based + Logical Clocks

## System Components

### Fault Tolerance & Failure Detection (Nikini Bandara)
- Log redundancy via replication
- Heartbeat-based failure detection
- Automatic failover and log recovery for rejoining nodes
- Performance and storage overhead logged

### Replication & Consistency (Sithija Oshan)
- Quorum-based replication 
- Strong consistency with deduplication 
- Optimized log retrieval 
- Latency and storage efficiency analysis

### Time Synchronization (Madara Chamudini)
- NTP-based synchronization with logical clocks 
- Clock skew handling and timestamp correction 
- Reordering out-of-sequence logs
- Trade-off analysis for synchronization accuracy vs. overhead

### Consensus (Pavan Kumarage)
- Custom Raft implementation
- Leader election for log coordination 
- Performance evaluation under high log ingestion rates
- Optimizations for consensus overhead

## Repository Structure

```
grpc-logger/
├── .idea/                       # IntelliJ IDEA project settings
├── .vscode/                     # VS Code settings
├── pom.xml                      # Maven build configuration
├── raft_log_50051.json          # Persistent log files for each node (per port)
├── raft_log_50052.json
├── raft_log_50053.json
├── raft_log_50054.json
├── raft_state_50051.json        # Persistent state files for each node (per port)
├── raft_state_50052.json
├── raft_state_50053.json
├── raft_state_50054.json
├── src/
│   └── main/
│       └── java/
│           ├── client/
│           │   └── RaftClient/                  # Client-side Raft interaction logic
│           ├── raft/
│           │   ├── core/
│           │   │   ├── RaftNode/                # Main Raft node implementation
│           │   │   └── RaftRole/                # Enum or logic for Raft roles (Leader, Follower, Candidate)
│           │   ├── grpc/
│           │   │   ├── ClientServiceImpl/       # gRPC services for client interaction
│           │   │   └── LogServiceImpl/          # gRPC services for inter-node communication
│           │   ├── model/
│           │   │   ├── LogEntryPOJO/            # Data model for log entries
│           │   │   └── RaftState/               # Data model for persistent state
│           │   ├── persistence/
│           │   │   ├── JsonRaftPersistence/     # JSON-based log and state persistence
│           │   │   └── RaftPersistence/         # Interface or base for persistence layer
│           │   ├── NodeServer/                  # Entry point or setup for Raft server node
│           │   └── util/
│           │       └── TimeSyncUtil/            # Utilities for time synchronization, etc.
│           └── proto/
│               ├── client.proto                 # gRPC definitions for client communication
│               └── logging.proto                # gRPC definitions for logging/replication
├── test/
│   ├── java/
│   ├── logging/                                 # Tests for logging service
│   └── raft/                                    # Tests for Raft logic
├── target/                                      # Maven build output directory
├── External Libraries/                          # Dependencies loaded via Maven
└── Scratches and Consoles/                      # IDE notes and scratch files

```

## Prerequisites

* **Java:** JDK 23
* **Maven:** Version 4.0.0 or later
* **gRPC & Protobuf:** Configured via Maven dependencies (`pom.xml`)
* **Protobuf Files:** Located in `src/main/java/proto/` (`client.proto`, `logging.proto`)
* **Operating System:** Tested on Windows 11
* **Additional Tools:** NTP server for time synchronization (e.g., `pool.ntp.org`)
* **Optional:** Docker (for containerized node deployment, if needed)

---

## Installation Instructions

1. **Clone the Repository:**

   ```bash
   git clone https://github.com/KumarageKPV/distributed-logging-system.git
   cd distributed-logging-system
   ```

2. **Install Dependencies:**

   ```bash
   mvn clean install
   ```

3. **Configure Environment:**

   * Define cluster nodes (`localhost:50051`, `localhost:50052`, etc.) inside your application logic or configuration service.
   * Persistent logs and states are managed via:

     * `raft_log_<port>.json`
     * `raft_state_<port>.json`
   * Use a reachable NTP server in the `TimeSyncUtil` module if enabling time synchronization (default: `pool.ntp.org`).

---

## Running the Prototype

### Method 1: Manual Execution

1. **Build the Project:**

   ```bash
   mvn package
   ```

2. **Start Raft Node Servers:**
   Each node can be started individually using a specified port:

   ```bash
   mvn "exec:java" "-Dexec.args=50051"
   mvn "exec:java" "-Dexec.args=50052"
   mvn "exec:java" "-Dexec.args=50053"

   ```

3. **Start the Client:**
   Send logs to the cluster:

   ```bash
   mvn exec:java '-Dexec.mainClass=client.RaftClient'
   ```

---

### Method 2: Using the Batch File (Windows)

Alternatively, you can run everything (nodes + client) with a single command using the provided batch script:

```bash
start_raft_system.bat
```

This will:

* Open 3 separate terminals for Raft nodes on ports `50051`, `50052`, and `50053`
* Launch the client to send logs to the cluster

---

## Example Usage

```bash
# Start server nodes (in different terminals or scripts)
mvn "exec:java" '-Dexec.mainClass=raft.NodeServer' "-Dexec.args=50051"
mvn "exec:java" '-Dexec.mainClass=raft.NodeServer' "-Dexec.args=50052"
mvn "exec:java" '-Dexec.mainClass=raft.NodeServer' "-Dexec.args=50053"

# Start a client and send a log
mvn exec:java '-Dexec.mainClass=client.RaftClient'

# Logs will be replicated and stored in raft_log_<port>.json across nodes
```

---

## Testing Scenarios

* **Fault Tolerance:**
  Terminate a node (e.g., port `50052`) and observe if another leader is elected and logs are still processed.

* **Replication:**
  Ensure that logs submitted from the client appear in all node-specific log files.

* **Time Synchronization:**
  Skew system clocks and check how `TimeSyncUtil` aligns timestamps in replicated entries.

* **Consensus Logic:**
  Force a network partition or delay heartbeat messages to simulate leader re-elections.

---

## Troubleshooting

* **Ports Not Binding:**
  Ensure ports `50051–50054` are available and not blocked by firewall or other services.

* **Time Sync Issues:**
  Confirm internet access and NTP server availability (`pool.ntp.org`). Check logs from `TimeSyncUtil`.

* **gRPC Failures:**
  Make sure Protobuf files are compiled correctly and that service stubs are generated. Check `proto/` and `grpc/` packages.

* **Log Inconsistencies:**
  Validate that `JsonRaftPersistence` writes are successful and not corrupted.

* **Consensus Failures:**
  Review leader election logic in `RaftNode` and log state in `RaftState`.

* **Maven Errors:**
  Ensure JDK 23 is configured and run:

  ```bash
  mvn clean install -U
  ```

---

## Contact

For support or questions, reach out to the team:
📧 [it23178540@my.sliit.lk](mailto:it23178540@my.sliit.lk)
