package cc.towerdefence.velocity.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FriendCache {
    private final Map<UUID, List<UUID>> friendMap = new ConcurrentHashMap<>();

    public List<UUID> get(UUID playerId) {
        return new ArrayList<>(this.friendMap.get(playerId));
    }

    public void set(UUID playerId, List<UUID> friends) {
        this.friendMap.put(playerId, friends);
    }

    public void add(UUID playerId, UUID friendId) {
        this.friendMap.get(playerId).add(friendId);
    }

    public void remove(UUID playerId, UUID friendId) {
        this.friendMap.get(playerId).remove(friendId);
    }

    public void removeAll(UUID playerId) {
        this.friendMap.remove(playerId);
    }

}
