@echo off
echo Starting Raft server nodes...

REM Start each server in a new terminal window
start "Node 50051" cmd /k mvn exec:java -Dexec.mainClass=raft.NodeServer -Dexec.args=50051
start "Node 50052" cmd /k mvn exec:java -Dexec.mainClass=raft.NodeServer -Dexec.args=50052
start "Node 50053" cmd /k mvn exec:java -Dexec.mainClass=raft.NodeServer -Dexec.args=50053

REM Optional: wait a few seconds to ensure servers are up
timeout /t 5 /nobreak >nul

echo Running client...
mvn exec:java -Dexec.mainClass=client.RaftClient

pause
