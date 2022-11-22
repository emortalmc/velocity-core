package cc.towerdefence.velocity.grpc.service;

import com.velocitypowered.api.proxy.ProxyServer;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GrpcServerContainer {
    private static final int PORT = 9090;

    private final Server server;

    public GrpcServerContainer(ProxyServer proxy) {
        this.server = ServerBuilder.forPort(PORT)
                .addService(new PrivateMessageReceiverService(proxy))
                .addService(new VelocityFriendService(proxy))
                .addService(new VelocityServerService(proxy))
                .addService(new VelocityPlayerTransporterService(proxy))
                .build();

        try {
            this.server.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        this.server.shutdownNow();
        try {
            this.server.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
