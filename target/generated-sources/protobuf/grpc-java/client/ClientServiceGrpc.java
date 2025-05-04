package client;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.58.0)",
    comments = "Source: client.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class ClientServiceGrpc {

  private ClientServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "client.ClientService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<client.Client.ClientCommandRequest,
      client.Client.ClientCommandResponse> getSendCommandMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SendCommand",
      requestType = client.Client.ClientCommandRequest.class,
      responseType = client.Client.ClientCommandResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<client.Client.ClientCommandRequest,
      client.Client.ClientCommandResponse> getSendCommandMethod() {
    io.grpc.MethodDescriptor<client.Client.ClientCommandRequest, client.Client.ClientCommandResponse> getSendCommandMethod;
    if ((getSendCommandMethod = ClientServiceGrpc.getSendCommandMethod) == null) {
      synchronized (ClientServiceGrpc.class) {
        if ((getSendCommandMethod = ClientServiceGrpc.getSendCommandMethod) == null) {
          ClientServiceGrpc.getSendCommandMethod = getSendCommandMethod =
              io.grpc.MethodDescriptor.<client.Client.ClientCommandRequest, client.Client.ClientCommandResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SendCommand"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  client.Client.ClientCommandRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  client.Client.ClientCommandResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ClientServiceMethodDescriptorSupplier("SendCommand"))
              .build();
        }
      }
    }
    return getSendCommandMethod;
  }

  private static volatile io.grpc.MethodDescriptor<client.Client.LogQueryRequest,
      client.Client.LogQueryResponse> getQueryLogsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "queryLogs",
      requestType = client.Client.LogQueryRequest.class,
      responseType = client.Client.LogQueryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<client.Client.LogQueryRequest,
      client.Client.LogQueryResponse> getQueryLogsMethod() {
    io.grpc.MethodDescriptor<client.Client.LogQueryRequest, client.Client.LogQueryResponse> getQueryLogsMethod;
    if ((getQueryLogsMethod = ClientServiceGrpc.getQueryLogsMethod) == null) {
      synchronized (ClientServiceGrpc.class) {
        if ((getQueryLogsMethod = ClientServiceGrpc.getQueryLogsMethod) == null) {
          ClientServiceGrpc.getQueryLogsMethod = getQueryLogsMethod =
              io.grpc.MethodDescriptor.<client.Client.LogQueryRequest, client.Client.LogQueryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "queryLogs"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  client.Client.LogQueryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  client.Client.LogQueryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ClientServiceMethodDescriptorSupplier("queryLogs"))
              .build();
        }
      }
    }
    return getQueryLogsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<client.Client.Empty,
      client.Client.IsLeaderResponse> getIsLeaderMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "IsLeader",
      requestType = client.Client.Empty.class,
      responseType = client.Client.IsLeaderResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<client.Client.Empty,
      client.Client.IsLeaderResponse> getIsLeaderMethod() {
    io.grpc.MethodDescriptor<client.Client.Empty, client.Client.IsLeaderResponse> getIsLeaderMethod;
    if ((getIsLeaderMethod = ClientServiceGrpc.getIsLeaderMethod) == null) {
      synchronized (ClientServiceGrpc.class) {
        if ((getIsLeaderMethod = ClientServiceGrpc.getIsLeaderMethod) == null) {
          ClientServiceGrpc.getIsLeaderMethod = getIsLeaderMethod =
              io.grpc.MethodDescriptor.<client.Client.Empty, client.Client.IsLeaderResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "IsLeader"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  client.Client.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  client.Client.IsLeaderResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ClientServiceMethodDescriptorSupplier("IsLeader"))
              .build();
        }
      }
    }
    return getIsLeaderMethod;
  }

  private static volatile io.grpc.MethodDescriptor<client.Client.Empty,
      client.Client.ServerTimeResponse> getGetServerTimeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetServerTime",
      requestType = client.Client.Empty.class,
      responseType = client.Client.ServerTimeResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<client.Client.Empty,
      client.Client.ServerTimeResponse> getGetServerTimeMethod() {
    io.grpc.MethodDescriptor<client.Client.Empty, client.Client.ServerTimeResponse> getGetServerTimeMethod;
    if ((getGetServerTimeMethod = ClientServiceGrpc.getGetServerTimeMethod) == null) {
      synchronized (ClientServiceGrpc.class) {
        if ((getGetServerTimeMethod = ClientServiceGrpc.getGetServerTimeMethod) == null) {
          ClientServiceGrpc.getGetServerTimeMethod = getGetServerTimeMethod =
              io.grpc.MethodDescriptor.<client.Client.Empty, client.Client.ServerTimeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetServerTime"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  client.Client.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  client.Client.ServerTimeResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ClientServiceMethodDescriptorSupplier("GetServerTime"))
              .build();
        }
      }
    }
    return getGetServerTimeMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ClientServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ClientServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ClientServiceStub>() {
        @java.lang.Override
        public ClientServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ClientServiceStub(channel, callOptions);
        }
      };
    return ClientServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ClientServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ClientServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ClientServiceBlockingStub>() {
        @java.lang.Override
        public ClientServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ClientServiceBlockingStub(channel, callOptions);
        }
      };
    return ClientServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ClientServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ClientServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ClientServiceFutureStub>() {
        @java.lang.Override
        public ClientServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ClientServiceFutureStub(channel, callOptions);
        }
      };
    return ClientServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     * Client sends a command to the leader
     * </pre>
     */
    default void sendCommand(client.Client.ClientCommandRequest request,
        io.grpc.stub.StreamObserver<client.Client.ClientCommandResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSendCommandMethod(), responseObserver);
    }

    /**
     * <pre>
     * Client queries logs within a time range
     * </pre>
     */
    default void queryLogs(client.Client.LogQueryRequest request,
        io.grpc.stub.StreamObserver<client.Client.LogQueryResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getQueryLogsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Client checks if connected node is leader
     * </pre>
     */
    default void isLeader(client.Client.Empty request,
        io.grpc.stub.StreamObserver<client.Client.IsLeaderResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getIsLeaderMethod(), responseObserver);
    }

    /**
     */
    default void getServerTime(client.Client.Empty request,
        io.grpc.stub.StreamObserver<client.Client.ServerTimeResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetServerTimeMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service ClientService.
   */
  public static abstract class ClientServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return ClientServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service ClientService.
   */
  public static final class ClientServiceStub
      extends io.grpc.stub.AbstractAsyncStub<ClientServiceStub> {
    private ClientServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ClientServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ClientServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Client sends a command to the leader
     * </pre>
     */
    public void sendCommand(client.Client.ClientCommandRequest request,
        io.grpc.stub.StreamObserver<client.Client.ClientCommandResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSendCommandMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Client queries logs within a time range
     * </pre>
     */
    public void queryLogs(client.Client.LogQueryRequest request,
        io.grpc.stub.StreamObserver<client.Client.LogQueryResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getQueryLogsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Client checks if connected node is leader
     * </pre>
     */
    public void isLeader(client.Client.Empty request,
        io.grpc.stub.StreamObserver<client.Client.IsLeaderResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getIsLeaderMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getServerTime(client.Client.Empty request,
        io.grpc.stub.StreamObserver<client.Client.ServerTimeResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetServerTimeMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service ClientService.
   */
  public static final class ClientServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<ClientServiceBlockingStub> {
    private ClientServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ClientServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ClientServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Client sends a command to the leader
     * </pre>
     */
    public client.Client.ClientCommandResponse sendCommand(client.Client.ClientCommandRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSendCommandMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Client queries logs within a time range
     * </pre>
     */
    public client.Client.LogQueryResponse queryLogs(client.Client.LogQueryRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getQueryLogsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Client checks if connected node is leader
     * </pre>
     */
    public client.Client.IsLeaderResponse isLeader(client.Client.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getIsLeaderMethod(), getCallOptions(), request);
    }

    /**
     */
    public client.Client.ServerTimeResponse getServerTime(client.Client.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetServerTimeMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service ClientService.
   */
  public static final class ClientServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<ClientServiceFutureStub> {
    private ClientServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ClientServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ClientServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Client sends a command to the leader
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<client.Client.ClientCommandResponse> sendCommand(
        client.Client.ClientCommandRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSendCommandMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Client queries logs within a time range
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<client.Client.LogQueryResponse> queryLogs(
        client.Client.LogQueryRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getQueryLogsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Client checks if connected node is leader
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<client.Client.IsLeaderResponse> isLeader(
        client.Client.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getIsLeaderMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<client.Client.ServerTimeResponse> getServerTime(
        client.Client.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetServerTimeMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SEND_COMMAND = 0;
  private static final int METHODID_QUERY_LOGS = 1;
  private static final int METHODID_IS_LEADER = 2;
  private static final int METHODID_GET_SERVER_TIME = 3;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SEND_COMMAND:
          serviceImpl.sendCommand((client.Client.ClientCommandRequest) request,
              (io.grpc.stub.StreamObserver<client.Client.ClientCommandResponse>) responseObserver);
          break;
        case METHODID_QUERY_LOGS:
          serviceImpl.queryLogs((client.Client.LogQueryRequest) request,
              (io.grpc.stub.StreamObserver<client.Client.LogQueryResponse>) responseObserver);
          break;
        case METHODID_IS_LEADER:
          serviceImpl.isLeader((client.Client.Empty) request,
              (io.grpc.stub.StreamObserver<client.Client.IsLeaderResponse>) responseObserver);
          break;
        case METHODID_GET_SERVER_TIME:
          serviceImpl.getServerTime((client.Client.Empty) request,
              (io.grpc.stub.StreamObserver<client.Client.ServerTimeResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getSendCommandMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              client.Client.ClientCommandRequest,
              client.Client.ClientCommandResponse>(
                service, METHODID_SEND_COMMAND)))
        .addMethod(
          getQueryLogsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              client.Client.LogQueryRequest,
              client.Client.LogQueryResponse>(
                service, METHODID_QUERY_LOGS)))
        .addMethod(
          getIsLeaderMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              client.Client.Empty,
              client.Client.IsLeaderResponse>(
                service, METHODID_IS_LEADER)))
        .addMethod(
          getGetServerTimeMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              client.Client.Empty,
              client.Client.ServerTimeResponse>(
                service, METHODID_GET_SERVER_TIME)))
        .build();
  }

  private static abstract class ClientServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ClientServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return client.Client.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ClientService");
    }
  }

  private static final class ClientServiceFileDescriptorSupplier
      extends ClientServiceBaseDescriptorSupplier {
    ClientServiceFileDescriptorSupplier() {}
  }

  private static final class ClientServiceMethodDescriptorSupplier
      extends ClientServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    ClientServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (ClientServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ClientServiceFileDescriptorSupplier())
              .addMethod(getSendCommandMethod())
              .addMethod(getQueryLogsMethod())
              .addMethod(getIsLeaderMethod())
              .addMethod(getGetServerTimeMethod())
              .build();
        }
      }
    }
    return result;
  }
}
