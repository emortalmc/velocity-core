package dev.emortal.velocity.general;

import dev.emortal.api.service.ServerDiscoveryGrpc;
import dev.emortal.api.service.ServerDiscoveryProto;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import dev.emortal.velocity.CorePlugin;
import dev.emortal.velocity.api.event.server.SwapToTowerDefenceEvent;
import dev.emortal.velocity.api.event.transport.PlayerTransportEvent;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;

public class ServerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerManager.class);

    private final ServerDiscoveryGrpc.ServerDiscoveryFutureStub serverDiscoveryService;
    private final ProxyServer proxy;

    public ServerManager(CorePlugin plugin, ProxyServer proxy) {
        this.serverDiscoveryService = GrpcStubCollection.getServerDiscoveryService().orElse(null);
        this.proxy = proxy;

        this.proxy.getEventManager().register(plugin, this);
    }

    public void sendToLobbyServer(Player player) {
        ListenableFuture<ServerDiscoveryProto.LobbyServer> lobbyServerFuture = this.serverDiscoveryService.getSuggestedLobbyServer(
                ServerDiscoveryProto.ServerRequest.newBuilder().setPlayerCount(1).build());

        Futures.addCallback(lobbyServerFuture, FunctionalFutureCallback.create(
                lobbyServer -> {
                    ServerDiscoveryProto.ConnectableServer connectableServer = lobbyServer.getConnectableServer();
                    this.connectPlayerToServer(player, connectableServer);
                },
                throwable -> LOGGER.error("Failed to get lobby server", throwable)
        ), ForkJoinPool.commonPool());
    }

    @Subscribe
    public void handlePlayerTransport(PlayerTransportEvent event) {
        for (UUID playerId : event.players()) {
            this.proxy.getPlayer(playerId).ifPresent(player -> {
                this.connectPlayerToServer(player, event.server());
            });
        }
    }

    @Subscribe
    public void onTowerDefenceChange(SwapToTowerDefenceEvent event) {
        this.sendToTowerDefenceServer(event.player(), event.quickJoin());
    }

    public void sendToTowerDefenceServer(Player player, boolean quickJoin) {
        ListenableFuture<ServerDiscoveryProto.ConnectableServer> tdServerFuture = this.serverDiscoveryService.getSuggestedTowerDefenceServer(
                ServerDiscoveryProto.TowerDefenceServerRequest.newBuilder()
                        .setInProgress(quickJoin)
                        .build());

        Futures.addCallback(tdServerFuture, FunctionalFutureCallback.create(
                connectableServer -> this.connectPlayerToServer(player, connectableServer),
                throwable -> LOGGER.error("Failed to get TD server", throwable)
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
