package dev.emortal.velocity.listener;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ListenerBoundEvent;
import dev.agones.sdk.AgonesSDKProto;
import dev.agones.sdk.SDKGrpc;
import dev.agones.sdk.alpha.AlphaAgonesSDKProto;
import dev.emortal.api.agonessdk.AgonesUtils;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class AgonesListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgonesListener.class);

    private final SDKGrpc.SDKFutureStub agonesService;
    private final SDKGrpc.SDKStub standardAgonesService;
    private final dev.agones.sdk.alpha.SDKGrpc.SDKFutureStub alphaAgonesService;

    public AgonesListener(SDKGrpc.SDKFutureStub agonesService, SDKGrpc.SDKStub standardAgonesService,
                          dev.agones.sdk.alpha.SDKGrpc.SDKFutureStub alphaAgonesService) {

        this.agonesService = agonesService;
        this.standardAgonesService = standardAgonesService;
        this.alphaAgonesService = alphaAgonesService;
    }

    @Subscribe
    public void onListenerBound(ListenerBoundEvent event) {
        ListenableFuture<AgonesSDKProto.Empty> readyResponse = this.agonesService.ready(AgonesSDKProto.Empty.getDefaultInstance());

        Futures.addCallback(readyResponse, FunctionalFutureCallback.create(
                result -> {
                },
                error -> LOGGER.error("Failed to set server to ready: ", error)
        ), ForkJoinPool.commonPool());

        AgonesUtils.startHealthTask(this.standardAgonesService, 5, TimeUnit.SECONDS);
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        ListenableFuture<AlphaAgonesSDKProto.Bool> response = this.alphaAgonesService.playerConnect(
                AlphaAgonesSDKProto.PlayerID.newBuilder().setPlayerID(event.getPlayer().getUniqueId().toString()).build()
        );

        Futures.addCallback(response, FunctionalFutureCallback.create(
                result -> {
                    if (!result.getBool()) LOGGER.warn("Failed to register player {} with Agones (already marked as logged in)", event.getPlayer().getUniqueId());
                },
                error -> LOGGER.error("Failed to set player to connected: ", error)
        ), ForkJoinPool.commonPool());
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        ListenableFuture<AlphaAgonesSDKProto.Bool> response = this.alphaAgonesService.playerDisconnect(
                AlphaAgonesSDKProto.PlayerID.newBuilder().setPlayerID(event.getPlayer().getUniqueId().toString()).build()
        );

        Futures.addCallback(response, FunctionalFutureCallback.create(
                result -> {
                    if (!result.getBool()) LOGGER.warn("Failed to unregister player {} with Agones (not marked as logged in)", event.getPlayer().getUniqueId());
                },
                error -> LOGGER.error("Failed to set player to disconnected: ", error)
        ), ForkJoinPool.commonPool());
    }
}
