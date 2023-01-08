package dev.emortal.velocity.listener;

import dev.emortal.velocity.cache.SessionCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Empty;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.service.McPlayerGrpc;
import dev.emortal.api.service.McPlayerProto;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.ForkJoinPool;

public class McPlayerListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(McPlayerListener.class);

    private final McPlayerGrpc.McPlayerFutureStub mcPlayerService;
    private final SessionCache sessionCache;

    public McPlayerListener(SessionCache sessionCache) {
        this.mcPlayerService = GrpcStubCollection.getPlayerService().orElse(null);
        this.sessionCache = sessionCache;
    }

    @Subscribe
    public void onJoin(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        Instant joinTime = Instant.now();

        ListenableFuture<McPlayerProto.PlayerLoginResponse> future = this.mcPlayerService.onPlayerLogin(McPlayerProto.McPlayerLoginRequest.newBuilder()
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
        SessionCache.CachedSession cachedSession =this.sessionCache.remove(player.getUniqueId());
        if (cachedSession == null) return;

        ListenableFuture<Empty> future = this.mcPlayerService.onPlayerDisconnect(McPlayerProto.McPlayerDisconnectRequest.newBuilder()
                .setPlayerId(String.valueOf(player.getUniqueId()))
                .setSessionId(cachedSession.sessionId())
                .build());

        Futures.addCallback(future, FunctionalFutureCallback.create(
                response -> {},
                error -> LOGGER.warn("Failed to unregister session for {}: {}", player.getUniqueId(), error)
        ), ForkJoinPool.commonPool());
    }
}
