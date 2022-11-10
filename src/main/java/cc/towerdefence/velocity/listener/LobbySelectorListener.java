package cc.towerdefence.velocity.listener;

import cc.towerdefence.api.model.common.PlayerProto;
import cc.towerdefence.api.service.McPlayerGrpc;
import cc.towerdefence.api.service.McPlayerProto;
import cc.towerdefence.api.service.ServerDiscoveryGrpc;
import cc.towerdefence.api.service.ServerDiscoveryProto;
import cc.towerdefence.api.utils.utils.FunctionalFutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Empty;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ForkJoinPool;

public class LobbySelectorListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(LobbySelectorListener.class);

    private final ServerDiscoveryGrpc.ServerDiscoveryFutureStub serverDiscoveryService;
    private final McPlayerGrpc.McPlayerFutureStub mcPlayerService;
    private final ProxyServer proxy;

    public LobbySelectorListener(ServerDiscoveryGrpc.ServerDiscoveryFutureStub serverDiscoveryService,
                                 McPlayerGrpc.McPlayerFutureStub mcPlayerService, ProxyServer proxy) {
        this.serverDiscoveryService = serverDiscoveryService;
        this.mcPlayerService = mcPlayerService;
        this.proxy = proxy;
    }

    @Subscribe
    public void onInitialServerChoose(PlayerChooseInitialServerEvent event, Continuation continuation) {
        ListenableFuture<McPlayerProto.PlayerResponse> playerResponseFuture = this.mcPlayerService.getPlayer(PlayerProto.PlayerRequest.newBuilder()
                .setPlayerId(String.valueOf(event.getPlayer().getUniqueId())).build());

        Futures.addCallback(playerResponseFuture, FunctionalFutureCallback.create(
                player -> {
                    boolean otpEnabled = player.getOtpEnabled();
                    if (!otpEnabled)
                        this.sendToLobbyServer(event, continuation);
                    else
                        this.sendToOtpServer(event, continuation);


                },
                throwable -> continuation.resumeWithException(throwable)
        ), ForkJoinPool.commonPool());

    }

    private void sendToOtpServer(PlayerChooseInitialServerEvent event, Continuation continuation) {

    }

    private void sendToLobbyServer(PlayerChooseInitialServerEvent event, Continuation continuation) {
        ListenableFuture<ServerDiscoveryProto.LobbyServer> lobbyServerFuture = this.serverDiscoveryService.getSuggestedLobbyServer(Empty.getDefaultInstance());

        Futures.addCallback(lobbyServerFuture, FunctionalFutureCallback.create(
                lobbyServer -> {
                    ServerDiscoveryProto.ConnectableServer connectableServer = lobbyServer.getConnectableServer();
                    RegisteredServer registeredServer = this.proxy.getServer(connectableServer.getId()).orElseGet(() -> {
                        InetSocketAddress address = new InetSocketAddress(connectableServer.getAddress(), connectableServer.getPort());
                        return this.proxy.registerServer(new ServerInfo(connectableServer.getId(), address));
                    });
                    event.setInitialServer(registeredServer);
                    continuation.resume();
                },
                throwable -> {
                    continuation.resumeWithException(throwable);
                }
        ), ForkJoinPool.commonPool());
    }
}
