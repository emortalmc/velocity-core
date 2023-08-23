package dev.emortal.velocity.adapter.player;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;

public final class VelocityPlayerProvider implements PlayerProvider {

    private final ProxyServer proxy;

    public VelocityPlayerProvider(@NotNull ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public @Nullable Player getPlayer(@NotNull UUID uuid) {
        return this.proxy.getPlayer(uuid).orElse(null);
    }

    @Override
    public @Nullable Player getPlayer(@NotNull String username) {
        return this.proxy.getPlayer(username).orElse(null);
    }

    @Override
    public @NotNull Collection<Player> allPlayers() {
        return this.proxy.getAllPlayers();
    }
}
