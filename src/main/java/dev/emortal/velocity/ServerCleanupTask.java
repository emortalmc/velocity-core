package dev.emortal.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ServerCleanupTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerCleanupTask.class);

    public ServerCleanupTask(CorePlugin plugin, @NotNull ProxyServer proxyServer) {
        proxyServer.getScheduler().buildTask(plugin, () -> this.cleanupServers(proxyServer))
                .repeat(5, TimeUnit.MINUTES)
                .schedule();
    }

    private void cleanupServers(@NotNull ProxyServer proxyServer) {
        List<RegisteredServer> unRegisteredServers = new ArrayList<>();
        for (RegisteredServer server : proxyServer.getAllServers()) {
            if (server.getPlayersConnected().size() == 0) {
                proxyServer.unregisterServer(server.getServerInfo());
                unRegisteredServers.add(server);
            }
        }

        if (unRegisteredServers.size() > 0) {
            LOGGER.info("Unregistered {} servers:", unRegisteredServers.size());
            for (RegisteredServer server : unRegisteredServers) {
                LOGGER.info(" - {}", server.getServerInfo().getName());
            }
        }
    }
}
