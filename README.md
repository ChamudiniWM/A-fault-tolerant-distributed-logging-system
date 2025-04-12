# 🗂️ Distributed Logging System

A **fault-tolerant**, **time-synchronized**, and **replicated** distributed logging platform built as part of our Distributed Systems group project.

## 👥 Team Members

| Name               | Responsibility               |
|--------------------|-------------------------------|
| **Pavan Kumarage** | Consensus (Paxos/Raft)        |
| **Sithija Oshan**  | Replication & Consistency     |
| **Madara Chamudini** | Time Synchronization        |
| **Nikini Bandara** | Fault Tolerance & Failure Detection |

---

## 🛠️ Tech Stack

- **Language:** Java 17  
- **Communication:** gRPC + Protobuf  
- **Build Tool:** Maven  
- **Consensus:** Custom Raft (basic version)  
- **Time Sync:** NTP-based + Logical clocks

---

## 🧱 Folder Structure

```
/src
  ├── main/java/com/group6/logsystem/
  │     ├── grpc/             # gRPC service classes
  │     ├── node/             # Node.java (core class)
  │     ├── consensus/        # Paxos / Raft logic
  │     ├── replication/      # Log replication logic
  │     ├── faulttolerance/   # Heartbeat / node health
  │     └── timesync/         # Timestamp and clock management
/tests                        # Unit & integration tests
/docs                         # Report, diagrams, drafts
/slides                       # Presentation materials
```

---

## 🚀 Setup & Run

1. Clone the repo  
   ```bash
   git clone https://github.com/your-org/distributed-logging-system.git
   cd distributed-logging-system
   ```

2. Build the project  
   ```bash
   mvn clean install
   ```

3. Run the gRPC server (or nodes)  
   ```bash
   Right-click `GrpcServer.java` → Run (in IntelliJ)
   ```

---

## 🧪 Testing

Run unit tests:
```bash
mvn test
```

Make sure you have test dependencies installed (JUnit, Mockito). If not, check the `pom.xml`.

---

## 🌐 Collaboration Guidelines

- Branch per feature/module:  
  - `consensus/` → `feature/consensus`  
  - `replication/` → `feature/replication`  
  - etc.

- Always pull `main` before pushing your branch:
  ```bash
  git checkout main
  git pull
  git checkout feature/your-branch
  git merge main
  ```

---

## 📬 Contact & Support

Open issues for bugs or questions, or reach out via your group chat. Contributions must go through pull requests.

---

> _Built with grit, bugs, and a dash of distributed magic_ ✨
