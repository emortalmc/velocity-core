package cc.towerdefence.velocity.listener;

import cc.towerdefence.api.service.PlayerTrackerGrpc;
import cc.towerdefence.api.service.PlayerTrackerProto;
import cc.towerdefence.api.utils.utils.FunctionalFutureCallback;
import cc.towerdefence.velocity.CorePlugin;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Empty;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;

public class PlayerTrackerListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(McPlayerListener.class);

    private final PlayerTrackerGrpc.PlayerTrackerFutureStub playerTrackerService;

    public PlayerTrackerListener(PlayerTrackerGrpc.PlayerTrackerFutureStub playerTrackerService) {
        this.playerTrackerService = playerTrackerService;
    }

    @Subscribe
    public void onJoin(PostLoginEvent event) {
        Player player = event.getPlayer();

        ListenableFuture<Empty> future = this.playerTrackerService.proxyPlayerLogin(PlayerTrackerProto.PlayerLoginRequest.newBuilder()
                .setPlayerId(String.valueOf(player.getUniqueId()))
                .setPlayerName(player.getUsername())
                .setServerId(CorePlugin.SERVER_ID)
                .build());

        Futures.addCallback(future, FunctionalFutureCallback.create(
                response -> {},
                error -> LOGGER.warn("Failed to register PlayerTracker session for {}: {}", player.getUniqueId(), error)
        ), ForkJoinPool.commonPool());
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();

        ListenableFuture<Empty> future = this.playerTrackerService.proxyPlayerDisconnect(PlayerTrackerProto.PlayerDisconnectRequest.newBuilder()
                .setPlayerId(String.valueOf(player.getUniqueId()))
                .setServerId(CorePlugin.SERVER_ID)
                .build());

        Futures.addCallback(future, FunctionalFutureCallback.create(
                response -> {},
                error -> LOGGER.warn("Failed to unregister session for {}: {}", player.getUniqueId(), error)
        ), ForkJoinPool.commonPool());
    }
}
