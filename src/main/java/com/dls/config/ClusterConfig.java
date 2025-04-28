package com.dls.config;

import com.dls.common.NodeInfo;
import java.util.List;

public class ClusterConfig {
    private List<NodeInfo> nodes;

    public ClusterConfig(List<NodeInfo> nodes) {
        this.nodes = nodes;
    }

    public List<NodeInfo> getNodes() {
        return nodes;
    }

    public void setNodes(List<NodeInfo> nodes) {
        this.nodes = nodes;
    }

    @Override
    public String toString() {
        return "ClusterConfig{" +
                "nodes=" + nodes +
                '}';
    }
}
