package dev.emortal.velocity.grpc.service;

import dev.emortal.api.service.velocity.VelocityPrivateMessageGrpc;
import dev.emortal.api.service.velocity.VelocityPrivateMessageProto;
import dev.emortal.velocity.api.event.PrivateMessageReceivedEvent;
import com.google.protobuf.Empty;
import com.velocitypowered.api.proxy.ProxyServer;
import io.grpc.stub.StreamObserver;

import java.util.UUID;

public class PrivateMessageReceiverService extends VelocityPrivateMessageGrpc.VelocityPrivateMessageImplBase {
    private final ProxyServer server;

    public PrivateMessageReceiverService(ProxyServer server) {
        this.server = server;
    }

    // Note: This is separated from the handler using events to make them easier to pull apart in the future.
    // This may be done if we want a single gRPC server for the Velocity proxy, but multiple plugins to use it.
    @Override
    public void receiveMessage(VelocityPrivateMessageProto.PrivateMessage request, StreamObserver<Empty> responseObserver) {
        this.server.getEventManager().fire(
                new PrivateMessageReceivedEvent(request.getSenderUsername(), UUID.fromString(request.getRecipientId()), request.getMessage())
        );
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
