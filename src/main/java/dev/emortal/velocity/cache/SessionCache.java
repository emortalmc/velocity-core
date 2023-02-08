package dev.emortal.velocity.cache;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionCache {
    private final Map<UUID, CachedSession> sessionIds = new ConcurrentHashMap<>();

    public void put(UUID playerId, CachedSession sessionId) {
        this.sessionIds.put(playerId, sessionId);
    }

    public CachedSession get(UUID playerId) {
        return this.sessionIds.get(playerId);
    }

    public CachedSession remove(UUID playerId) {
        return this.sessionIds.remove(playerId);
    }

    public record CachedSession(Instant loginTime) {}

    @Subscribe
    public void onPlayerConnect(PostLoginEvent event) {
        this.put(event.getPlayer().getUniqueId(), new CachedSession(Instant.now()));
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        this.remove(event.getPlayer().getUniqueId());
    }
}
