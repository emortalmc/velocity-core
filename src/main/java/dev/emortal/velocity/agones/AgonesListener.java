package dev.emortal.velocity.agones;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ListenerBoundEvent;
import dev.agones.sdk.AgonesSDKProto;
import dev.agones.sdk.SDKGrpc;
import dev.agones.sdk.alpha.AlphaAgonesSDKProto;
import dev.agones.sdk.beta.BetaAgonesSDKProto;
import dev.emortal.api.agonessdk.AgonesUtils;
import io.grpc.StatusRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

final class AgonesListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgonesListener.class);

    private final SDKGrpc.SDKBlockingStub agonesService;
    private final SDKGrpc.SDKStub standardAgonesService;
    private final dev.agones.sdk.beta.SDKGrpc.SDKBlockingStub betaSdk;

    AgonesListener(@NotNull AgonesGrpcStubCollection stubManager) {
        this.agonesService = stubManager.getAgonesService();
        this.standardAgonesService = stubManager.getStandardAgonesService();
        this.betaSdk = stubManager.getBetaAgonesService();
    }

    @Subscribe(async = true)
    void onListenerBound(@NotNull ListenerBoundEvent event) {
        try {
            this.agonesService.ready(AgonesSDKProto.Empty.getDefaultInstance());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to set server to ready: ", exception);
        }

        AgonesUtils.startHealthTask(this.standardAgonesService, 5, TimeUnit.SECONDS);
    }

    @Subscribe(async = true)
    void onLogin(@NotNull LoginEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        AlphaAgonesSDKProto.PlayerID playerId = AlphaAgonesSDKProto.PlayerID.newBuilder().setPlayerID(id.toString()).build();

        this.updateAgonesCounter("players", 1);
        this.addToAgonesList("players", id.toString());
    }

    @Subscribe(async = true)
    void onDisconnect(@NotNull DisconnectEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        AlphaAgonesSDKProto.PlayerID playerId = AlphaAgonesSDKProto.PlayerID.newBuilder().setPlayerID(id.toString()).build();

        this.updateAgonesCounter("players", -1);
        this.removeFromAgonesList("players", id.toString());
    }

    public void updateAgonesCounter(String name, long diff) {
        try {
            this.betaSdk.updateCounter(BetaAgonesSDKProto.UpdateCounterRequest.newBuilder()
                    .setCounterUpdateRequest(BetaAgonesSDKProto.CounterUpdateRequest.newBuilder()
                            .setName(name)
                            .setCountDiff(diff))
                    .build());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to update counter: ", exception);
        }
    }

    public void addToAgonesList(String listName, String value) {
        try {
            this.betaSdk.addListValue(BetaAgonesSDKProto.AddListValueRequest.newBuilder()
                    .setName(listName)
                    .setValue(value)
                    .build());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to add to list: ", exception);
        }
    }

    public void removeFromAgonesList(String listName, String value) {
        try {
            this.betaSdk.removeListValue(BetaAgonesSDKProto.RemoveListValueRequest.newBuilder()
                    .setName(listName)
                    .setValue(value)
                    .build());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to remove from list: ", exception);
        }
    }
}
