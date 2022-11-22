package cc.towerdefence.velocity.listener;

import cc.towerdefence.api.service.McPlayerGrpc;
import cc.towerdefence.api.service.McPlayerProto;
import cc.towerdefence.api.service.ServerDiscoveryGrpc;
import cc.towerdefence.api.service.ServerDiscoveryProto;
import cc.towerdefence.api.utils.GrpcStubCollection;
import cc.towerdefence.api.utils.utils.FunctionalFutureCallback;
import cc.towerdefence.velocity.grpc.stub.GrpcStubManager;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Empty;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ForkJoinPool;

public class LobbySelectorListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(LobbySelectorListener.class);

    private final ServerDiscoveryGrpc.ServerDiscoveryFutureStub serverDiscoveryService;
    private final McPlayerGrpc.McPlayerFutureStub mcPlayerService;
    private final ProxyServer proxy;
    private final OtpEventListener otpEventListener;

    public LobbySelectorListener(GrpcStubManager stubManager, ProxyServer proxy,
                                 OtpEventListener otpEventListener) {
        this.serverDiscoveryService = GrpcStubCollection.getServerDiscoveryService().orElse(null);
        this.mcPlayerService = GrpcStubCollection.getPlayerService().orElse(null);
        this.proxy = proxy;
        this.otpEventListener = otpEventListener;
    }

    @Subscribe
    public void onInitialServerChoose(PlayerChooseInitialServerEvent event, Continuation continuation) {
        ListenableFuture<McPlayerProto.PlayerResponse> playerResponseFuture = this.mcPlayerService.getPlayer(McPlayerProto.PlayerRequest.newBuilder()
                .setPlayerId(String.valueOf(event.getPlayer().getUniqueId())).build());

        Futures.addCallback(playerResponseFuture, FunctionalFutureCallback.create(
                player -> {
                    boolean otpEnabled = player.getOtpEnabled();
                    if (!otpEnabled)
                        this.sendToLobbyServer(event, continuation);
                    else
                        this.sendToOtpServer(event, continuation);
                },
                throwable -> {
                    Status status = Status.fromThrowable(throwable);
                    if (status == Status.NOT_FOUND) this.sendToLobbyServer(event, continuation);
                    else continuation.resumeWithException(throwable);
                }
        ), ForkJoinPool.commonPool());
    }

    private void sendToOtpServer(PlayerChooseInitialServerEvent event, Continuation continuation) {
        this.otpEventListener.getRestrictedPlayers().add(event.getPlayer().getUniqueId());

        ListenableFuture<ServerDiscoveryProto.ConnectableServer> otpServerFuture = this.serverDiscoveryService.getSuggestedOtpServer(Empty.getDefaultInstance());

        Futures.addCallback(otpServerFuture, FunctionalFutureCallback.create(
                server -> {
                    this.connectPlayerToServer(event, server);
                    continuation.resume();
                },
                continuation::resumeWithException
        ), ForkJoinPool.commonPool());
    }

    private void sendToLobbyServer(PlayerChooseInitialServerEvent event, Continuation continuation) {
        ListenableFuture<ServerDiscoveryProto.LobbyServer> lobbyServerFuture = this.serverDiscoveryService.getSuggestedLobbyServer(
                ServerDiscoveryProto.ServerRequest.newBuilder().setPlayerCount(1).build()
        );

        Futures.addCallback(lobbyServerFuture, FunctionalFutureCallback.create(
                lobbyServer -> {
                    ServerDiscoveryProto.ConnectableServer connectableServer = lobbyServer.getConnectableServer();
                    this.connectPlayerToServer(event, connectableServer);
                    continuation.resume();
                },
                continuation::resumeWithException
        ), ForkJoinPool.commonPool());
    }

    private void connectPlayerToServer(PlayerChooseInitialServerEvent event, ServerDiscoveryProto.ConnectableServer connectableServer) {
        RegisteredServer registeredServer = this.proxy.getServer(connectableServer.getId()).orElseGet(() -> {
            InetSocketAddress address = new InetSocketAddress(connectableServer.getAddress(), connectableServer.getPort());
            return this.proxy.registerServer(new ServerInfo(connectableServer.getId(), address));
        });
        event.setInitialServer(registeredServer);
    }
}
