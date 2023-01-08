package dev.emortal.velocity.grpc.service;

import dev.emortal.api.service.velocity.VelocityPlayerTransporterGrpc;
import dev.emortal.api.service.velocity.VelocityPlayerTransporterProto;
import dev.emortal.velocity.api.event.transport.PlayerTransportEvent;
import com.google.protobuf.Empty;
import com.velocitypowered.api.proxy.ProxyServer;
import io.grpc.stub.StreamObserver;

import java.util.UUID;
import java.util.stream.Collectors;

public class VelocityPlayerTransporterService extends VelocityPlayerTransporterGrpc.VelocityPlayerTransporterImplBase {
    private final ProxyServer server;

    public VelocityPlayerTransporterService(ProxyServer proxyServer) {
        this.server = proxyServer;
    }

    @Override
    public void sendToServer(VelocityPlayerTransporterProto.TransportRequest request, StreamObserver<Empty> responseObserver) {
        this.server.getEventManager().fire(
                new PlayerTransportEvent(
                        request.getPlayerIdsList().stream().map(UUID::fromString).collect(Collectors.toUnmodifiableSet()), request.getServer()
                )
        );

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
