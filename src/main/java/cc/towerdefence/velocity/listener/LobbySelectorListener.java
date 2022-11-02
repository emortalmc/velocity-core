package cc.towerdefence.velocity.listener;

import cc.towerdefence.api.model.server.ConnectableServerProto;
import cc.towerdefence.api.model.server.LobbyServerProto;
import cc.towerdefence.api.service.ServerDiscoveryGrpc;
import cc.towerdefence.api.utils.utils.FunctionalFutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Empty;
import com.velocitypowered.api.event.AwaitingEventExecutor;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

public class LobbySelectorListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(LobbySelectorListener.class);

    private final ServerDiscoveryGrpc.ServerDiscoveryFutureStub serverDiscoveryService;
    private final ProxyServer proxy;

    public LobbySelectorListener(ServerDiscoveryGrpc.ServerDiscoveryFutureStub serverDiscoveryService, ProxyServer proxy) {
        this.serverDiscoveryService = serverDiscoveryService;
        this.proxy = proxy;
    }

    @Subscribe
    public void onInitialServerChoose(PlayerChooseInitialServerEvent event, Continuation continuation) {
        System.out.printf("Performing onInitialServerChoose for %s on thread %s%n", event.getPlayer().getUsername(), Thread.currentThread().getName());
        ListenableFuture<LobbyServerProto.LobbyServer> lobbyServerFuture = this.serverDiscoveryService.getSuggestedLobbyServer(Empty.getDefaultInstance());

        Futures.addCallback(lobbyServerFuture, FunctionalFutureCallback.create(
                lobbyServer -> {
                    ConnectableServerProto.ConnectableServer connectableServer = lobbyServer.getConnectableServer();
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
