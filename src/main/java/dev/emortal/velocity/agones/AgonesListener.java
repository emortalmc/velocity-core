package dev.emortal.velocity.agones;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ListenerBoundEvent;
import dev.agones.sdk.AgonesSDKProto;
import dev.agones.sdk.SDKGrpc;
import dev.agones.sdk.alpha.AlphaAgonesSDKProto;
import dev.emortal.api.agonessdk.AgonesUtils;
import io.grpc.StatusRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class AgonesListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgonesListener.class);

    private final SDKGrpc.SDKBlockingStub agonesService;
    private final SDKGrpc.SDKStub standardAgonesService;
    private final dev.agones.sdk.alpha.SDKGrpc.SDKBlockingStub alphaAgonesService;

    public AgonesListener(@NotNull AgonesGrpcStubCollection stubManager) {
        this.agonesService = stubManager.getAgonesService();
        this.standardAgonesService = stubManager.getStandardAgonesService();
        this.alphaAgonesService = stubManager.getAlphaAgonesService();
    }

    @Subscribe(async = true)
    public void onListenerBound(@NotNull ListenerBoundEvent event) {
        try {
            this.agonesService.ready(AgonesSDKProto.Empty.getDefaultInstance());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to set server to ready: ", exception);
        }

        AgonesUtils.startHealthTask(this.standardAgonesService, 5, TimeUnit.SECONDS);
    }

    @Subscribe(async = true)
    public void onLogin(@NotNull LoginEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        AlphaAgonesSDKProto.PlayerID playerId = AlphaAgonesSDKProto.PlayerID.newBuilder().setPlayerID(id.toString()).build();

        boolean success;
        try {
            success = this.alphaAgonesService.playerConnect(playerId).getBool();
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to set player to connected: ", exception);
            return;
        }

        if (!success) LOGGER.warn("Failed to register player {} with Agones (already marked as logged in)", id);
    }

    @Subscribe(async = true)
    public void onDisconnect(@NotNull DisconnectEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        AlphaAgonesSDKProto.PlayerID playerId = AlphaAgonesSDKProto.PlayerID.newBuilder().setPlayerID(id.toString()).build();

        boolean success;
        try {
            success = this.alphaAgonesService.playerDisconnect(playerId).getBool();
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to set player to disconnected: ", exception);
            return;
        }

        if (!success) LOGGER.warn("Failed to unregister player {} with Agones (not marked as logged in)", id);
    }
}
