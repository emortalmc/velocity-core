package dev.emortal.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerCleanupTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerCleanupTask.class);

    private final ProxyServer proxyServer;

    public ServerCleanupTask(@NotNull ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Subscribe
    public void onPlayerDisconnect(@NotNull DisconnectEvent event) {
        event.getPlayer().getCurrentServer().ifPresent(conn -> this.checkServer(conn.getServer()));
    }

    @Subscribe
    public void onPlayerDisconnect(@NotNull ServerConnectedEvent event) {
        event.getPreviousServer().ifPresent(this::checkServer);
    }

    private void checkServer(@NotNull RegisteredServer server) {
        if (server.getPlayersConnected().size() == 0) {
            this.proxyServer.unregisterServer(server.getServerInfo());
            LOGGER.info("Unregistered server {}", server.getServerInfo().getName());
        }
    }
}
