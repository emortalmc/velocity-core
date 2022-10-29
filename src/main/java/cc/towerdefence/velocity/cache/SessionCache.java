package cc.towerdefence.velocity.cache;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionCache {
    private final Map<UUID, String> sessionIds = new ConcurrentHashMap<>();

    public void put(UUID playerId, String sessionId) {
        this.sessionIds.put(playerId, sessionId);
    }

    public String get(UUID playerId) {
        return this.sessionIds.get(playerId);
    }

    public String remove(UUID playerId) {
        return this.sessionIds.remove(playerId);
    }
}
