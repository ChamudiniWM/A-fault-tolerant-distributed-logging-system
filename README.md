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

- **Language:** Java 17
- **Communication:** gRPC + Protobuf
- **Build Tool:** Maven
- **Consensus:** Raft
- **Time Synchronization:** NTP-based + Logical Clocks

## System Components

### Fault Tolerance & Failure Detection (Nikini Bandara)
- Log redundancy via replication (FailoverManager, RecoveryManager)
- Heartbeat-based failure detection (FailureDetector)
- Automatic failover and log recovery for rejoining nodes
- Performance and storage overhead logged in performance_log.csv

### Replication & Consistency (Sithija Oshan)
- Quorum-based replication (ReplicationManager)
- Eventual consistency with deduplication (DeduplicationService, ConsistencyChecker)
- Optimized log retrieval (LogRetriever)
- Latency and storage efficiency analysis

### Time Synchronization (Madara Chamudini)
- NTP-based synchronization with logical clocks (TimeSynchronizer)
- Clock skew handling and timestamp correction (ClockSkewHandler, TimestampCorrector)
- Reordering out-of-sequence logs
- Trade-off analysis for synchronization accuracy vs. overhead

### Consensus (Pavan Kumarage)
- Custom Raft implementation (ConsensusModule, RaftNode, RaftLog)
- Leader election for log coordination (LeaderElection)
- Performance evaluation under high log ingestion rates
- Optimizations for consensus overhead (RaftState)

## Repository Structure

```
distributed-logging-system/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/dls/
│   │   │       ├── client/                  # Client-side logic
│   │   │       │   ├── ClientMain
│   │   │       │   ├── LoggingClient
│   │   │       │   └── PeerClient
│   │   │       ├── common/                  # Shared utilities
│   │   │       │   ├── CustomExceptions
│   │   │       │   ├── LocalLogEntry
│   │   │       │   ├── NodeInfo
│   │   │       │   ├── PerformanceLogger
│   │   │       │   ├── RejectionTracker
│   │   │       │   └── TimeUtils
│   │   │       ├── config/                  # Configuration management
│   │   │       │   ├── ClusterConfig
│   │   │       │   └── ConfigLoader
│   │   │       ├── faulttolerance/          # Fault tolerance mechanisms
│   │   │       │   ├── FailoverManager
│   │   │       │   ├── FailureDetector
│   │   │       │   └── RecoveryManager
│   │   │       ├── loadtest/                # Load testing utilities
│   │   │       │   ├── LoadTestClient
│   │   │       │   └── PerformanceMetrics
│   │   │       ├── raft/                    # Raft consensus implementation
│   │   │       │   ├── rpc/
│   │   │       │   ├── ConsensusModule
│   │   │       │   ├── LeaderElection
│   │   │       │   ├── RaftLog
│   │   │       │   ├── RaftNode
│   │   │       │   └── RaftState
│   │   │       ├── replication/             # Replication and consistency
│   │   │       │   ├── ConsistencyChecker
│   │   │       │   ├── DeduplicationService
│   │   │       │   ├── LogRetriever
│   │   │       │   └── ReplicationManager
│   │   │       ├── server/                  # Server-side logic
│   │   │       │   ├── LoggingServer
│   │   │       │   ├── MultiNodeServerMain
│   │   │       │   ├── ServerConfig
│   │   │       │   └── ServerMain
│   │   │       └── timasync/                # Time synchronization
│   │   │           ├── ClockSkewHandler
│   │   │           ├── TimestampCorrector
│   │   │           └── TimeSynchronizer
│   │   ├── proto/                           # Protocol definitions
│   │   │   └── raft.proto
│   │   └── resources/                       # Configuration files
│   │       ├── application.properties
│   │       ├── cluster_config.json
│   │       └── logback.xml
│   └── test/
│       └── java/
│           └── com/group6/logsystem/
│               ├── Consensus/
│               ├── tests/
│               ├── TimeSync/
│               └── TimeSyncRaftBehaviorTests
├── target/
│   ├── qftignore
│   └── performance_log.csv
└── pom.xml                                    # Maven build configuration
```

## Prerequisites

- **Java:** JDK 17
- **Maven:** Version 3.8.0 or later
- **gRPC:** Configured via Maven dependencies (see `pom.xml`)
- **Protobuf:** Protocol definitions in `src/main/proto/raft.proto`
- **Operating System:** Tested on Windows 11
- **Additional Tools:** NTP server for time synchronization (e.g., `pool.ntp.org`)
- **Optional:** Docker for running distributed nodes

## Installation Instructions

1. **Clone the Repository:**  
   `git clone [[repository-url]](https://github.com/KumarageKPV/distributed-logging-system.git)`  
   `cd distributed-logging-system`

2. **Install Dependencies:**  
   `mvn clean install`

3. **Set Up Environment:**
    - Update `src/main/resources/cluster_config.json` with node IPs and ports (e.g., `localhost:5000`, `localhost:5001`, `localhost:5002`).
    - Configure `application.properties` for logging levels or NTP server settings (e.g., `ntp.server=pool.ntp.org`).
    - Ensure network connectivity for gRPC communication (open ports, e.g., `5000-5002`).

## Running the Prototype

1. **Build the Project:**  
   `mvn package`

2. **Start the Logging Server** (required before nodes):  
   `java -cp target/distributed-logging-system.jar com.dls.server.LoggingServer`

3. **Start Server Nodes** (choose one method):

    - Option 1: Start individual nodes with `ServerMain`:  
      `java -cp target/distributed-logging-system.jar com.dls.server.ServerMain 5000`  
      `java -cp target/distributed-logging-system.jar com.dls.server.ServerMain 5001`  
      `java -cp target/distributed-logging-system.jar com.dls.server.ServerMain 5002`

    - Option 2: Start multiple nodes with `MultiNodeServerMain`:  
      `java -cp target/distributed-logging-system.jar com.dls.server.MultiNodeServerMain`

4. **Start the Client:**  
   `java -cp target/distributed-logging-system.jar com.dls.client.ClientMain --server localhost:5000`

5. **Run Tests:**  
   `mvn test`

   Tests cover consensus (Consensus/), time synchronization (TimeSync/), and Raft behavior (TimeSyncRaftBehaviorTests)

   Simulate failures by stopping `LoggingServer` or a node to test fault tolerance

## Example Usage

1. **Start the logging server:**  
   `java -cp target/distributed-logging-system.jar com.dls.server.LoggingServer`

2. **Start three server nodes using MultiNodeServerMain:**  
   `java -cp target/distributed-logging-system.jar com.dls.server.MultiNodeServerMain`

3. **Send a log from the client:**  
   `java -cp target/distributed-logging-system.jar com.dls.client.ClientMain --server

localhost:5000 --log "Example log entry"`

4. **Verify Logs:**  
   Logs are replicated and synchronized across nodes with fault tolerance. You can query logs from any server node


## Testing Scenarios

- **Fault Tolerance:** Stop a node (e.g., port `5001`) and verify log redirection and recovery (FailoverManager, RecoveryManager)
- **Replication:** Send logs and confirm replication across nodes (ReplicationManager)
- **Time Synchronization:** Introduce clock skew and verify log ordering (TimestampCorrector, TimeSynchronizer)
- **Consensus:** Simulate network partitions and verify leader election (LeaderElection, ConsensusModule)

## Troubleshooting

- **Connection Issues:** Ensure ports (5000-5002) are open and nodes are listed in `cluster_config.json`
- **Time Sync Errors:** Verify NTP server accessibility (`pool.ntp.org`) or check `TimeSynchronizer` logs
- **Consensus Failures:** Confirm Raft quorum settings in `RaftNode` and check for network partitions
- **Logs Not Replicated:** Verify `ReplicationManager` and `DeduplicationService` configurations
- **gRPC Errors:** Ensure `raft.proto` is compiled and dependencies are resolved via `mvn install`
- **Maven Build Errors:** Run `mvn clean install` and confirm JDK 17 is used

## Contact

For questions, contact the team via email: [it23178540@my.sliit.lk](mailto:it23178540@my.sliit.lk)
