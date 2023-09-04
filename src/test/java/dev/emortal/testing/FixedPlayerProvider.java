package dev.emortal.testing;

import com.velocitypowered.api.proxy.Player;
import dev.emortal.velocity.adapter.player.PlayerProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record FixedPlayerProvider(@Nullable Player player) implements PlayerProvider {

    @Override
    public @Nullable Player getPlayer(@NotNull UUID uuid) {
        return this.player;
    }

    @Override
    public @Nullable Player getPlayer(@NotNull String username) {
        return this.player;
    }

    @Override
    public @NotNull Collection<Player> allPlayers() {
        if (this.player == null) return List.of();
        return List.of(this.player);
    }

    @Override
    public int playerCount() {
        return 1;
    }
}
