package cc.towerdefence.velocity.listener;

import cc.towerdefence.api.service.McPlayerGrpc;
import cc.towerdefence.api.service.McPlayerProto;
import cc.towerdefence.api.utils.utils.FunctionalFutureCallback;
import cc.towerdefence.velocity.cache.SessionCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Empty;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.ForkJoinPool;

public class McPlayerListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(McPlayerListener.class);

    private final McPlayerGrpc.McPlayerFutureStub mcPlayerService;
    private final SessionCache sessionCache;

    public McPlayerListener(McPlayerGrpc.McPlayerFutureStub mcPlayerService, SessionCache sessionCache) {
        this.mcPlayerService = mcPlayerService;
        this.sessionCache = sessionCache;
    }

    @Subscribe
    public void onJoin(PostLoginEvent event) {
        Player player = event.getPlayer();
        Instant joinTime = Instant.now();

        ListenableFuture<McPlayerProto.PlayerLoginResponse> future = this.mcPlayerService.onPlayerLogin(McPlayerProto.PlayerLoginRequest.newBuilder()
                .setPlayerId(String.valueOf(player.getUniqueId()))
                .setUsername(player.getUsername())
                .build());

        Futures.addCallback(future, FunctionalFutureCallback.create(
                response -> this.sessionCache.put(player.getUniqueId(), new SessionCache.CachedSession(response.getSessionId(), joinTime)),
                error -> LOGGER.warn("Failed to register McPlayer session for {}: {}", player.getUniqueId(), error)
        ), ForkJoinPool.commonPool());
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();

        ListenableFuture<Empty> future = this.mcPlayerService.onPlayerDisconnect(McPlayerProto.PlayerDisconnectRequest.newBuilder()
                .setPlayerId(String.valueOf(player.getUniqueId()))
                .setSessionId(this.sessionCache.remove(player.getUniqueId()).sessionId())
                .build());

        Futures.addCallback(future, FunctionalFutureCallback.create(
                response -> {},
                error -> LOGGER.warn("Failed to unregister session for {}: {}", player.getUniqueId(), error)
        ), ForkJoinPool.commonPool());
    }
}
