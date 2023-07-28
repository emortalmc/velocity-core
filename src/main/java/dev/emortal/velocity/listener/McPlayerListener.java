package dev.emortal.velocity.listener;

import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.velocity.cache.SessionCache;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class McPlayerListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(McPlayerListener.class);

    private final McPlayerService playerService;
    private final SessionCache sessionCache;

    public McPlayerListener(@NotNull McPlayerService playerService, @NotNull SessionCache sessionCache) {
        this.playerService = playerService;
        this.sessionCache = sessionCache;
    }

    // TODO: Re-implement. We don't need to call the API but the SessionCache needs to be aware of when a player logs in.
//    @Subscribe
//    public void onJoin(ServerPostConnectEvent event) {
//        Player player = event.getPlayer();
//        Instant joinTime = Instant.now();
//
//        var loginResponseFuture = this.mcPlayerService.onPlayerLogin(McPlayerProto.McPlayerLoginRequest.newBuilder()
//                .setPlayerId(String.valueOf(player.getUniqueId()))
//                .setUsername(player.getUsername())
//                .build());
//
//        Futures.addCallback(loginResponseFuture, FunctionalFutureCallback.create(
//                response -> this.sessionCache.put(player.getUniqueId(), new SessionCache.CachedSession(response.getSessionId(), joinTime)),
//                error -> LOGGER.warn("Failed to register McPlayer session for {}: {}", player.getUniqueId(), error)
//        ), ForkJoinPool.commonPool());
//    }
}
