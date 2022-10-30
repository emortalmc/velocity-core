package cc.towerdefence.velocity.grpc.service;

import cc.towerdefence.api.service.velocity.VelocityFriendGrpc;
import cc.towerdefence.api.service.velocity.VelocityFriendProto;
import cc.towerdefence.velocity.api.event.friend.FriendAddReceivedEvent;
import cc.towerdefence.velocity.api.event.friend.FriendRemoveReceivedEvent;
import cc.towerdefence.velocity.api.event.friend.FriendRequestReceivedEvent;
import com.google.protobuf.Empty;
import com.velocitypowered.api.proxy.ProxyServer;
import io.grpc.stub.StreamObserver;

import java.util.UUID;

public class VelocityFriendService extends VelocityFriendGrpc.VelocityFriendImplBase {
    private final ProxyServer server;

    public VelocityFriendService(ProxyServer server) {
        this.server = server;
    }

    @Override
    public void receiveFriendRequest(VelocityFriendProto.ReceiveFriendRequestRequest request, StreamObserver<Empty> responseObserver) {
        this.server.getEventManager().fire(
                new FriendRequestReceivedEvent(UUID.fromString(request.getSenderId()), request.getSenderUsername(), UUID.fromString(request.getRecipientId()))
        );
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void receiveFriendAdded(VelocityFriendProto.ReceiveFriendAddedRequest request, StreamObserver<Empty> responseObserver) {
        this.server.getEventManager().fire(
                new FriendAddReceivedEvent(UUID.fromString(request.getSenderId()), request.getSenderUsername(), UUID.fromString(request.getRecipientId()))
        );
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void receiveFriendRemoved(VelocityFriendProto.ReceiveFriendRemovedRequest request, StreamObserver<Empty> responseObserver) {
        this.server.getEventManager().fire(
                new FriendRemoveReceivedEvent(UUID.fromString(request.getRecipientId()), UUID.fromString(request.getSenderId()))
        );
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
