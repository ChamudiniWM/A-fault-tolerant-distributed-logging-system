package com.dls.server;

public class ServerConfig {
    private String host;
    private int port;
    private int electionTimeoutMin;
    private int electionTimeoutMax;

    // Constructor
    public ServerConfig(String host, int port, int electionTimeoutMin, int electionTimeoutMax) {
        this.host = host;
        this.port = port;
        this.electionTimeoutMin = electionTimeoutMin;
        this.electionTimeoutMax = electionTimeoutMax;
    }

    // Getters and Setters
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getElectionTimeoutMin() {
        return electionTimeoutMin;
    }

    public void setElectionTimeoutMin(int electionTimeoutMin) {
        this.electionTimeoutMin = electionTimeoutMin;
    }

    public int getElectionTimeoutMax() {
        return electionTimeoutMax;
    }

    public void setElectionTimeoutMax(int electionTimeoutMax) {
        this.electionTimeoutMax = electionTimeoutMax;
    }

    // Optional: Method to print config
    @Override
    public String toString() {
        return "ServerConfig{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", electionTimeoutMin=" + electionTimeoutMin +
                ", electionTimeoutMax=" + electionTimeoutMax +
                '}';
    }
}
