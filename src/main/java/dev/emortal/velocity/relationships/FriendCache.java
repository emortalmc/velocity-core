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

public final class FriendCache {

    private final RelationshipService relationshipService;
    private final Map<UUID, List<CachedFriend>> friendMap = new ConcurrentHashMap<>();

    public FriendCache(@NotNull RelationshipService relationshipService) {
        this.relationshipService = relationshipService;
    }

    public @NotNull List<CachedFriend> get(@NotNull UUID playerId) {
        return this.friendMap.get(playerId);
    }

    public void set(@NotNull UUID playerId, @NotNull List<CachedFriend> friends) {
        this.friendMap.put(playerId, friends);
    }

    public void add(@NotNull UUID playerId, @NotNull CachedFriend friend) {
        this.friendMap.get(playerId).add(friend);
    }

    public void remove(@NotNull UUID playerId, @NotNull UUID friendId) {
        this.friendMap.get(playerId).removeIf(cachedFriend -> cachedFriend.playerId().equals(friendId));
    }

    public void removeAll(@NotNull UUID playerId) {
        this.friendMap.remove(playerId);
    }

    @Subscribe
    public void onPlayerLogin(@NotNull PostLoginEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        List<Friend> friends;
        try {
            friends = this.relationshipService.listFriends(playerId);
        } catch (StatusRuntimeException exception) {
            this.removeAll(playerId);
            return;
        }

        List<CachedFriend> cachedFriends = new ArrayList<>();
        for (Friend(UUID id, Instant friendsSince) : friends) {
            cachedFriends.add(new CachedFriend(id, friendsSince));
        }

        this.set(playerId, cachedFriends);
    }

    @Subscribe
    public void onPlayerDisconnect(@NotNull DisconnectEvent event) {
        this.removeAll(event.getPlayer().getUniqueId());
    }

    public record CachedFriend(@NotNull UUID playerId, @NotNull Instant friendsSince) {
    }
}
