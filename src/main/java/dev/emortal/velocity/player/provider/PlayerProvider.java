package dev.emortal.velocity.player.provider;

import com.velocitypowered.api.proxy.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface PlayerProvider {

    @Nullable Player getPlayer(@NotNull UUID uuid);

    @Nullable Player getPlayer(@NotNull String username);
}
