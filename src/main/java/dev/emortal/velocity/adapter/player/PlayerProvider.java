package dev.emortal.velocity.adapter.player;

import com.velocitypowered.api.proxy.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;

public interface PlayerProvider {

    @Nullable Player getPlayer(@NotNull UUID uuid);

    @Nullable Player getPlayer(@NotNull String username);

    @NotNull Collection<Player> allPlayers();
}
