package com.dls.config;

import com.dls.common.NodeInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;

public class ConfigLoader {

    public static List<NodeInfo> loadClusterConfig(String fileName) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(fileName);
            if (is == null) {
                throw new RuntimeException("Configuration file not found: " + fileName);
            }
            return mapper.readValue(is, new TypeReference<List<NodeInfo>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to load cluster configuration: " + e.getMessage(), e);
        }
    }
}