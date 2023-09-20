package dev.emortal.velocity.adapter.server;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;

public final class VelocityServerProvider implements ServerProvider {

    private final @NotNull ProxyServer proxy;

    public VelocityServerProvider(@NotNull ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public @NotNull RegisteredServer createServer(@NotNull String name, @NotNull String address, int port) {
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);
        ServerInfo info = new ServerInfo(name, socketAddress);
        return this.proxy.createRawRegisteredServer(info);
    }
}
