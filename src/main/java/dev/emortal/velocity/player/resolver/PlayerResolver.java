package dev.emortal.velocity.player.resolver;

import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.model.mcplayer.McPlayer;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.velocity.adapter.player.PlayerProvider;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

public final class PlayerResolver {

    private final @Nullable McPlayerService playerService;
    private final @NotNull PlayerProvider playerProvider;

    public PlayerResolver(@Nullable McPlayerService playerService, @NotNull PlayerProvider playerProvider) {
        this.playerService = playerService;
        this.playerProvider = playerProvider;
    }

    @Blocking
    public @Nullable CachedMcPlayer getPlayer(@NotNull UUID uuid) throws StatusException {
        Player player = this.playerProvider.getPlayer(uuid);
        if (player != null) return this.convertPlayer(player);

        return this.requestPlayer(uuid);
    }

    @Blocking
    public @Nullable CachedMcPlayer getPlayer(@NotNull String username) throws StatusException {
        String usernameLowercase = username.toLowerCase(Locale.ROOT);

        Player player = this.playerProvider.getPlayer(usernameLowercase);
        if (player != null) return this.convertPlayer(player);

        return this.requestPlayer(usernameLowercase);
    }

    @Blocking
    private @Nullable CachedMcPlayer requestPlayer(@NotNull UUID uuid) throws StatusException {
        if (this.playerService == null) return null;

        McPlayer player;
        try {
            player = this.playerService.getPlayerById(uuid);
        } catch (StatusRuntimeException var4) {
            throw new StatusException(var4.getStatus(), var4.getTrailers());
        }

        return player != null ? this.convertPlayer(player) : null;
    }

    @Blocking
    private @Nullable CachedMcPlayer requestPlayer(@NotNull String username) throws StatusException {
        if (this.playerService == null) return null;

        McPlayer player;
        try {
            player = this.playerService.getPlayerByUsername(username);
        } catch (StatusRuntimeException var4) {
            throw new StatusException(var4.getStatus(), var4.getTrailers());
        }

        return player != null ? this.convertPlayer(player) : null;
    }

    private @NotNull CachedMcPlayer convertPlayer(@NotNull Player player) {
        return new CachedMcPlayer(player.getUniqueId(), player.getUsername(), true);
    }

    private @NotNull CachedMcPlayer convertPlayer(@NotNull McPlayer player) {
        return new CachedMcPlayer(UUID.fromString(player.getId()), player.getCurrentUsername(), player.hasCurrentServer());
    }
}
