package dev.emortal.testing;

import com.velocitypowered.api.proxy.Player;
import dev.emortal.velocity.player.provider.PlayerProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
}
