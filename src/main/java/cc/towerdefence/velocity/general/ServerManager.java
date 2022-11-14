package cc.towerdefence.velocity.general;

import cc.towerdefence.api.service.ServerDiscoveryGrpc;
import cc.towerdefence.api.service.ServerDiscoveryProto;
import cc.towerdefence.api.utils.utils.FunctionalFutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Empty;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ForkJoinPool;

public class ServerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerManager.class);

    private final ServerDiscoveryGrpc.ServerDiscoveryFutureStub serverDiscoveryService;
    private final ProxyServer proxy;

    public ServerManager(ServerDiscoveryGrpc.ServerDiscoveryFutureStub serverDiscoveryService, ProxyServer proxy) {
        this.serverDiscoveryService = serverDiscoveryService;
        this.proxy = proxy;
    }

    public void sendToLobbyServer(Player player) {
        ListenableFuture<ServerDiscoveryProto.LobbyServer> lobbyServerFuture = this.serverDiscoveryService.getSuggestedLobbyServer(Empty.getDefaultInstance());

        Futures.addCallback(lobbyServerFuture, FunctionalFutureCallback.create(
                lobbyServer -> {
                    ServerDiscoveryProto.ConnectableServer connectableServer = lobbyServer.getConnectableServer();
                    this.connectPlayerToServer(player, connectableServer);
                },
                throwable -> LOGGER.error("Failed to get lobby server", throwable)
        ), ForkJoinPool.commonPool());
    }

    private void connectPlayerToServer(Player player, ServerDiscoveryProto.ConnectableServer connectableServer) {
        RegisteredServer registeredServer = this.proxy.getServer(connectableServer.getId()).orElseGet(() -> {
            InetSocketAddress address = new InetSocketAddress(connectableServer.getAddress(), connectableServer.getPort());
            return this.proxy.registerServer(new ServerInfo(connectableServer.getId(), address));
        });
        player.createConnectionRequest(registeredServer).fireAndForget();
    }
}
