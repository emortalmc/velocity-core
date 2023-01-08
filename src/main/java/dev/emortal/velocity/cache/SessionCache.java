package dev.emortal.velocity.cache;

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

    public record CachedSession(String sessionId, Instant loginTime) {}
}
