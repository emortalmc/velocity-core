package dev.emortal.velocity.relationships;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import dev.emortal.api.service.relationship.Friend;
import dev.emortal.api.service.relationship.RelationshipService;
import io.grpc.StatusRuntimeException;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FriendCache {

    private final Map<UUID, List<CachedFriend>> friendMap = new ConcurrentHashMap<>();
    private final RelationshipService relationshipService;

    public FriendCache(@NotNull RelationshipService relationshipService) {
        this.relationshipService = relationshipService;
    }

    public List<CachedFriend> get(UUID playerId) {
        return this.friendMap.get(playerId);
    }

    public void set(UUID playerId, List<CachedFriend> friends) {
        this.friendMap.put(playerId, friends);
    }

    public void add(UUID playerId, CachedFriend friendId) {
        List<CachedFriend> friends = this.friendMap.get(playerId);
        friends.add(friendId);
    }

    public void remove(UUID playerId, UUID friendId) {
        this.friendMap.get(playerId).removeIf(cachedFriend -> cachedFriend.playerId().equals(friendId));
    }

    public void removeAll(UUID playerId) {
        this.friendMap.remove(playerId);
    }

    @Subscribe
    public void onPlayerLogin(PostLoginEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        List<Friend> friends;
        try {
            friends = this.relationshipService.listFriends(playerId);
        } catch (StatusRuntimeException exception) {
            this.removeAll(playerId);
            return;
        }

        List<CachedFriend> cachedFriends = new ArrayList<>();
        for (Friend friend : friends) {
            cachedFriends.add(new CachedFriend(friend.id(), friend.friendsSince()));
        }

        this.set(playerId, cachedFriends);
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        this.removeAll(event.getPlayer().getUniqueId());
    }

    public record CachedFriend(UUID playerId, Instant friendsSince) {
    }
}
