package com.group6.logsystem.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class GrpcServer {

    public static void main(String[] args) throws Exception {
        Server server = ServerBuilder
                .forPort(50051)
                .addService(new LogServiceImpl())
                .build();

        System.out.println("gRPC Server started on port 50051");
        server.start();
        server.awaitTermination();
    }
}
