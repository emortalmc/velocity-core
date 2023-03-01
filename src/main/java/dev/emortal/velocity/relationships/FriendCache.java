package dev.emortal.velocity.relationships;

import com.google.common.util.concurrent.Futures;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import dev.emortal.api.grpc.relationship.RelationshipGrpc;
import dev.emortal.api.grpc.relationship.RelationshipProto;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.api.utils.GrpcTimestampConverter;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public class FriendCache {
    private final Map<UUID, List<CachedFriend>> friendMap = new ConcurrentHashMap<>();
    private final RelationshipGrpc.RelationshipFutureStub relationshipService;

    public FriendCache() {
        this.relationshipService = GrpcStubCollection.getRelationshipService().orElse(null);
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
        String playerId = event.getPlayer().getUniqueId().toString();
        var response = this.relationshipService.getFriendList(
                RelationshipProto.GetFriendListRequest.newBuilder().setPlayerId(playerId).build()
        );

        Futures.addCallback(response, FunctionalFutureCallback.create(
                result -> {
                    this.set(event.getPlayer().getUniqueId(), result.getFriendsList().stream()
                            .map(friendListPlayer -> new CachedFriend(
                                    UUID.fromString(friendListPlayer.getId()),
                                    GrpcTimestampConverter.reverse(friendListPlayer.getFriendsSince())
                            )).collect(Collectors.toList()));
                },
                throwable -> this.removeAll(event.getPlayer().getUniqueId())
        ), ForkJoinPool.commonPool());
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        this.removeAll(event.getPlayer().getUniqueId());
    }

    public record CachedFriend(UUID playerId, Instant friendsSince) {
    }

}
