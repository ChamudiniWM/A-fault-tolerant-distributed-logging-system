package logging;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * --- Service Definition ---
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.58.0)",
    comments = "Source: logging.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class LogServiceGrpc {

  private LogServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "logging.LogService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<logging.PingRequest,
      logging.PingResponse> getPingMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Ping",
      requestType = logging.PingRequest.class,
      responseType = logging.PingResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<logging.PingRequest,
      logging.PingResponse> getPingMethod() {
    io.grpc.MethodDescriptor<logging.PingRequest, logging.PingResponse> getPingMethod;
    if ((getPingMethod = LogServiceGrpc.getPingMethod) == null) {
      synchronized (LogServiceGrpc.class) {
        if ((getPingMethod = LogServiceGrpc.getPingMethod) == null) {
          LogServiceGrpc.getPingMethod = getPingMethod =
              io.grpc.MethodDescriptor.<logging.PingRequest, logging.PingResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Ping"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  logging.PingRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  logging.PingResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LogServiceMethodDescriptorSupplier("Ping"))
              .build();
        }
      }
    }
    return getPingMethod;
  }

  private static volatile io.grpc.MethodDescriptor<logging.VoteRequest,
      logging.VoteResponse> getRequestVoteMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RequestVote",
      requestType = logging.VoteRequest.class,
      responseType = logging.VoteResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<logging.VoteRequest,
      logging.VoteResponse> getRequestVoteMethod() {
    io.grpc.MethodDescriptor<logging.VoteRequest, logging.VoteResponse> getRequestVoteMethod;
    if ((getRequestVoteMethod = LogServiceGrpc.getRequestVoteMethod) == null) {
      synchronized (LogServiceGrpc.class) {
        if ((getRequestVoteMethod = LogServiceGrpc.getRequestVoteMethod) == null) {
          LogServiceGrpc.getRequestVoteMethod = getRequestVoteMethod =
              io.grpc.MethodDescriptor.<logging.VoteRequest, logging.VoteResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RequestVote"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  logging.VoteRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  logging.VoteResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LogServiceMethodDescriptorSupplier("RequestVote"))
              .build();
        }
      }
    }
    return getRequestVoteMethod;
  }

  private static volatile io.grpc.MethodDescriptor<logging.AppendEntriesRequest,
      logging.AppendEntriesResponse> getAppendEntriesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AppendEntries",
      requestType = logging.AppendEntriesRequest.class,
      responseType = logging.AppendEntriesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<logging.AppendEntriesRequest,
      logging.AppendEntriesResponse> getAppendEntriesMethod() {
    io.grpc.MethodDescriptor<logging.AppendEntriesRequest, logging.AppendEntriesResponse> getAppendEntriesMethod;
    if ((getAppendEntriesMethod = LogServiceGrpc.getAppendEntriesMethod) == null) {
      synchronized (LogServiceGrpc.class) {
        if ((getAppendEntriesMethod = LogServiceGrpc.getAppendEntriesMethod) == null) {
          LogServiceGrpc.getAppendEntriesMethod = getAppendEntriesMethod =
              io.grpc.MethodDescriptor.<logging.AppendEntriesRequest, logging.AppendEntriesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AppendEntries"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  logging.AppendEntriesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  logging.AppendEntriesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LogServiceMethodDescriptorSupplier("AppendEntries"))
              .build();
        }
      }
    }
    return getAppendEntriesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<logging.LeaderUpdateRequest,
      logging.LeaderUpdateResponse> getNotifyLeaderUpdateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "notifyLeaderUpdate",
      requestType = logging.LeaderUpdateRequest.class,
      responseType = logging.LeaderUpdateResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<logging.LeaderUpdateRequest,
      logging.LeaderUpdateResponse> getNotifyLeaderUpdateMethod() {
    io.grpc.MethodDescriptor<logging.LeaderUpdateRequest, logging.LeaderUpdateResponse> getNotifyLeaderUpdateMethod;
    if ((getNotifyLeaderUpdateMethod = LogServiceGrpc.getNotifyLeaderUpdateMethod) == null) {
      synchronized (LogServiceGrpc.class) {
        if ((getNotifyLeaderUpdateMethod = LogServiceGrpc.getNotifyLeaderUpdateMethod) == null) {
          LogServiceGrpc.getNotifyLeaderUpdateMethod = getNotifyLeaderUpdateMethod =
              io.grpc.MethodDescriptor.<logging.LeaderUpdateRequest, logging.LeaderUpdateResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "notifyLeaderUpdate"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  logging.LeaderUpdateRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  logging.LeaderUpdateResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LogServiceMethodDescriptorSupplier("notifyLeaderUpdate"))
              .build();
        }
      }
    }
    return getNotifyLeaderUpdateMethod;
  }

  private static volatile io.grpc.MethodDescriptor<logging.GetLogInfoRequest,
      logging.GetLogInfoResponse> getGetLogInfoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetLogInfo",
      requestType = logging.GetLogInfoRequest.class,
      responseType = logging.GetLogInfoResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<logging.GetLogInfoRequest,
      logging.GetLogInfoResponse> getGetLogInfoMethod() {
    io.grpc.MethodDescriptor<logging.GetLogInfoRequest, logging.GetLogInfoResponse> getGetLogInfoMethod;
    if ((getGetLogInfoMethod = LogServiceGrpc.getGetLogInfoMethod) == null) {
      synchronized (LogServiceGrpc.class) {
        if ((getGetLogInfoMethod = LogServiceGrpc.getGetLogInfoMethod) == null) {
          LogServiceGrpc.getGetLogInfoMethod = getGetLogInfoMethod =
              io.grpc.MethodDescriptor.<logging.GetLogInfoRequest, logging.GetLogInfoResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetLogInfo"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  logging.GetLogInfoRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  logging.GetLogInfoResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LogServiceMethodDescriptorSupplier("GetLogInfo"))
              .build();
        }
      }
    }
    return getGetLogInfoMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static LogServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LogServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LogServiceStub>() {
        @java.lang.Override
        public LogServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LogServiceStub(channel, callOptions);
        }
      };
    return LogServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static LogServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LogServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LogServiceBlockingStub>() {
        @java.lang.Override
        public LogServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LogServiceBlockingStub(channel, callOptions);
        }
      };
    return LogServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static LogServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LogServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LogServiceFutureStub>() {
        @java.lang.Override
        public LogServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LogServiceFutureStub(channel, callOptions);
        }
      };
    return LogServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * --- Service Definition ---
   * </pre>
   */
  public interface AsyncService {

    /**
     */
    default void ping(logging.PingRequest request,
        io.grpc.stub.StreamObserver<logging.PingResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPingMethod(), responseObserver);
    }

    /**
     * <pre>
     * Raft leader election
     * </pre>
     */
    default void requestVote(logging.VoteRequest request,
        io.grpc.stub.StreamObserver<logging.VoteResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRequestVoteMethod(), responseObserver);
    }

    /**
     * <pre>
     * Raft log replication &amp; heartbeat
     * </pre>
     */
    default void appendEntries(logging.AppendEntriesRequest request,
        io.grpc.stub.StreamObserver<logging.AppendEntriesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAppendEntriesMethod(), responseObserver);
    }

    /**
     */
    default void notifyLeaderUpdate(logging.LeaderUpdateRequest request,
        io.grpc.stub.StreamObserver<logging.LeaderUpdateResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getNotifyLeaderUpdateMethod(), responseObserver);
    }

    /**
     */
    default void getLogInfo(logging.GetLogInfoRequest request,
        io.grpc.stub.StreamObserver<logging.GetLogInfoResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetLogInfoMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service LogService.
   * <pre>
   * --- Service Definition ---
   * </pre>
   */
  public static abstract class LogServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return LogServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service LogService.
   * <pre>
   * --- Service Definition ---
   * </pre>
   */
  public static final class LogServiceStub
      extends io.grpc.stub.AbstractAsyncStub<LogServiceStub> {
    private LogServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LogServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LogServiceStub(channel, callOptions);
    }

    /**
     */
    public void ping(logging.PingRequest request,
        io.grpc.stub.StreamObserver<logging.PingResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPingMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Raft leader election
     * </pre>
     */
    public void requestVote(logging.VoteRequest request,
        io.grpc.stub.StreamObserver<logging.VoteResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRequestVoteMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Raft log replication &amp; heartbeat
     * </pre>
     */
    public void appendEntries(logging.AppendEntriesRequest request,
        io.grpc.stub.StreamObserver<logging.AppendEntriesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAppendEntriesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void notifyLeaderUpdate(logging.LeaderUpdateRequest request,
        io.grpc.stub.StreamObserver<logging.LeaderUpdateResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getNotifyLeaderUpdateMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getLogInfo(logging.GetLogInfoRequest request,
        io.grpc.stub.StreamObserver<logging.GetLogInfoResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetLogInfoMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service LogService.
   * <pre>
   * --- Service Definition ---
   * </pre>
   */
  public static final class LogServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<LogServiceBlockingStub> {
    private LogServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LogServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LogServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public logging.PingResponse ping(logging.PingRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPingMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Raft leader election
     * </pre>
     */
    public logging.VoteResponse requestVote(logging.VoteRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRequestVoteMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Raft log replication &amp; heartbeat
     * </pre>
     */
    public logging.AppendEntriesResponse appendEntries(logging.AppendEntriesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAppendEntriesMethod(), getCallOptions(), request);
    }

    /**
     */
    public logging.LeaderUpdateResponse notifyLeaderUpdate(logging.LeaderUpdateRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getNotifyLeaderUpdateMethod(), getCallOptions(), request);
    }

    /**
     */
    public logging.GetLogInfoResponse getLogInfo(logging.GetLogInfoRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetLogInfoMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service LogService.
   * <pre>
   * --- Service Definition ---
   * </pre>
   */
  public static final class LogServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<LogServiceFutureStub> {
    private LogServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LogServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LogServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<logging.PingResponse> ping(
        logging.PingRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPingMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Raft leader election
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<logging.VoteResponse> requestVote(
        logging.VoteRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRequestVoteMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Raft log replication &amp; heartbeat
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<logging.AppendEntriesResponse> appendEntries(
        logging.AppendEntriesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAppendEntriesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<logging.LeaderUpdateResponse> notifyLeaderUpdate(
        logging.LeaderUpdateRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getNotifyLeaderUpdateMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<logging.GetLogInfoResponse> getLogInfo(
        logging.GetLogInfoRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetLogInfoMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_PING = 0;
  private static final int METHODID_REQUEST_VOTE = 1;
  private static final int METHODID_APPEND_ENTRIES = 2;
  private static final int METHODID_NOTIFY_LEADER_UPDATE = 3;
  private static final int METHODID_GET_LOG_INFO = 4;

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
        case METHODID_PING:
          serviceImpl.ping((logging.PingRequest) request,
              (io.grpc.stub.StreamObserver<logging.PingResponse>) responseObserver);
          break;
        case METHODID_REQUEST_VOTE:
          serviceImpl.requestVote((logging.VoteRequest) request,
              (io.grpc.stub.StreamObserver<logging.VoteResponse>) responseObserver);
          break;
        case METHODID_APPEND_ENTRIES:
          serviceImpl.appendEntries((logging.AppendEntriesRequest) request,
              (io.grpc.stub.StreamObserver<logging.AppendEntriesResponse>) responseObserver);
          break;
        case METHODID_NOTIFY_LEADER_UPDATE:
          serviceImpl.notifyLeaderUpdate((logging.LeaderUpdateRequest) request,
              (io.grpc.stub.StreamObserver<logging.LeaderUpdateResponse>) responseObserver);
          break;
        case METHODID_GET_LOG_INFO:
          serviceImpl.getLogInfo((logging.GetLogInfoRequest) request,
              (io.grpc.stub.StreamObserver<logging.GetLogInfoResponse>) responseObserver);
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
          getPingMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              logging.PingRequest,
              logging.PingResponse>(
                service, METHODID_PING)))
        .addMethod(
          getRequestVoteMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              logging.VoteRequest,
              logging.VoteResponse>(
                service, METHODID_REQUEST_VOTE)))
        .addMethod(
          getAppendEntriesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              logging.AppendEntriesRequest,
              logging.AppendEntriesResponse>(
                service, METHODID_APPEND_ENTRIES)))
        .addMethod(
          getNotifyLeaderUpdateMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              logging.LeaderUpdateRequest,
              logging.LeaderUpdateResponse>(
                service, METHODID_NOTIFY_LEADER_UPDATE)))
        .addMethod(
          getGetLogInfoMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              logging.GetLogInfoRequest,
              logging.GetLogInfoResponse>(
                service, METHODID_GET_LOG_INFO)))
        .build();
  }

  private static abstract class LogServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    LogServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return logging.LoggingProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("LogService");
    }
  }

  private static final class LogServiceFileDescriptorSupplier
      extends LogServiceBaseDescriptorSupplier {
    LogServiceFileDescriptorSupplier() {}
  }

  private static final class LogServiceMethodDescriptorSupplier
      extends LogServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    LogServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (LogServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new LogServiceFileDescriptorSupplier())
              .addMethod(getPingMethod())
              .addMethod(getRequestVoteMethod())
              .addMethod(getAppendEntriesMethod())
              .addMethod(getNotifyLeaderUpdateMethod())
              .addMethod(getGetLogInfoMethod())
              .build();
        }
      }
    }
    return result;
  }
}
