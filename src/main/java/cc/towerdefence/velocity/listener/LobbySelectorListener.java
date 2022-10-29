package cc.towerdefence.velocity.listener;

import cc.towerdefence.api.model.server.ConnectableServerProto;
import cc.towerdefence.api.model.server.LobbyServerProto;
import cc.towerdefence.api.service.ServerDiscoveryGrpc;
import com.google.protobuf.Empty;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

public class LobbySelectorListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(LobbySelectorListener.class);

    private final ServerDiscoveryGrpc.ServerDiscoveryFutureStub serverDiscoveryService;
    private final ProxyServer proxy;

    public LobbySelectorListener(ServerDiscoveryGrpc.ServerDiscoveryFutureStub serverDiscoveryService, ProxyServer proxy) {
        this.serverDiscoveryService = serverDiscoveryService;
        this.proxy = proxy;
    }

    @Subscribe
    public void onInitialServerChoose(PlayerChooseInitialServerEvent event) {
        try {
            LobbyServerProto.LobbyServer lobbyServer = this.serverDiscoveryService.getSuggestedLobbyServer(Empty.getDefaultInstance()).get();

            ConnectableServerProto.ConnectableServer connectableServer = lobbyServer.getConnectableServer();
            RegisteredServer registeredServer = this.proxy.getServer(connectableServer.getId()).orElseGet(() -> {
                InetSocketAddress address = new InetSocketAddress(connectableServer.getAddress(), connectableServer.getPort());
                return this.proxy.registerServer(new ServerInfo(connectableServer.getId(), address));
            });
            event.setInitialServer(registeredServer);
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Failed to get suggested lobby server: ", e);
        }

    }
}
