package dev.emortal.velocity.grpc.service;

import dev.emortal.api.service.velocity.VelocityServerGrpc;
import dev.emortal.api.service.velocity.VelocityServerProto;
import dev.emortal.velocity.api.event.server.SwapToTowerDefenceEvent;
import com.google.protobuf.Empty;
import com.velocitypowered.api.proxy.ProxyServer;
import io.grpc.stub.StreamObserver;

import java.util.UUID;

public class VelocityServerService extends VelocityServerGrpc.VelocityServerImplBase {
    private final ProxyServer server;

    public VelocityServerService(ProxyServer server) {
        this.server = server;
    }

    @Override
    public void swapTowerDefence(VelocityServerProto.TowerDefenceSwapRequest request, StreamObserver<Empty> responseObserver) {
        this.server.getPlayer(UUID.fromString(request.getPlayerId())).ifPresent(player -> {
            this.server.getEventManager().fire(new SwapToTowerDefenceEvent(player, request.getQuickJoin()));
        });

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
