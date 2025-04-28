package com.dls.common;

import java.util.Objects;

public class NodeInfo {
    private String nodeId;
    private String host;
    private int port;

    public NodeInfo(String nodeId, String host, int port) {
        this.nodeId = nodeId;
        this.host = host;
        this.port = port;
    }

    // Default constructor (optional)
    public NodeInfo() {
        this.nodeId = "";
        this.host = "localhost";
        this.port = 8080;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

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

    @Override
    public String toString() {
        return String.format("NodeInfo{nodeId='%s', host='%s', port=%d}", nodeId, host, port);
    }

    // Override equals and hashCode to compare nodes based on nodeId
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeInfo nodeInfo = (NodeInfo) o;
        return port == nodeInfo.port && nodeId.equals(nodeInfo.nodeId) && host.equals(nodeInfo.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, host, port);
    }
}
