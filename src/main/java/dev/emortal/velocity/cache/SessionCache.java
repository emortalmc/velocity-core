package dev.emortal.velocity.cache;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionCache {

    private final Map<UUID, CachedSession> sessionIds = new ConcurrentHashMap<>();

    public void put(@NotNull UUID playerId, @NotNull CachedSession sessionId) {
        this.sessionIds.put(playerId, sessionId);
    }

    public @Nullable CachedSession get(@NotNull UUID playerId) {
        return this.sessionIds.get(playerId);
    }

    public @Nullable CachedSession remove(@NotNull UUID playerId) {
        return this.sessionIds.remove(playerId);
    }

    public record CachedSession(@NotNull Instant loginTime) {}

    @Subscribe
    public void onPlayerConnect(@NotNull PostLoginEvent event) {
        this.put(event.getPlayer().getUniqueId(), new CachedSession(Instant.now()));
    }

    @Subscribe
    public void onPlayerDisconnect(@NotNull DisconnectEvent event) {
        this.remove(event.getPlayer().getUniqueId());
    }
}
