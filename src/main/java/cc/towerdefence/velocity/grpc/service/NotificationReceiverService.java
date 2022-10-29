package cc.towerdefence.velocity.grpc.service;

import cc.towerdefence.api.service.velocity.VelocityNotificationReceiverGrpc;
import cc.towerdefence.api.service.velocity.VelocityNotificationReceiverProto;
import cc.towerdefence.velocity.api.event.FriendRequestReceivedEvent;
import com.google.protobuf.Empty;
import com.velocitypowered.api.proxy.ProxyServer;
import io.grpc.stub.StreamObserver;

import java.util.UUID;

public class NotificationReceiverService extends VelocityNotificationReceiverGrpc.VelocityNotificationReceiverImplBase {
    private final ProxyServer server;

    public NotificationReceiverService(ProxyServer server) {
        this.server = server;
    }

    @Override
    public void receiveFriendRequest(VelocityNotificationReceiverProto.ReceiveFriendRequestRequest request, StreamObserver<Empty> responseObserver) {
        this.server.getEventManager().fire(
                new FriendRequestReceivedEvent(UUID.fromString(request.getSenderId()), request.getSenderUsername(), UUID.fromString(request.getRecipientId()))
        );
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
