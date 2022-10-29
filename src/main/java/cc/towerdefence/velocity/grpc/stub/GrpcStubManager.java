package cc.towerdefence.velocity.grpc.stub;

import cc.towerdefence.api.service.FriendGrpc;
import cc.towerdefence.api.service.McPlayerGrpc;
import cc.towerdefence.api.service.PlayerTrackerGrpc;
import cc.towerdefence.api.service.ServerDiscoveryGrpc;
import cc.towerdefence.velocity.CorePlugin;
import dev.agones.sdk.SDKGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.jetbrains.annotations.NotNull;

public class GrpcStubManager {
    private static final int AGONES_GRPC_PORT = CorePlugin.DEV_ENVIRONMENT ? 9357 : Integer.parseInt(System.getenv("AGONES_SDK_GRPC_PORT"));

    private final @NotNull PlayerTrackerGrpc.PlayerTrackerFutureStub playerTrackerService;
    private final @NotNull McPlayerGrpc.McPlayerFutureStub mcPlayerService;
    private final @NotNull FriendGrpc.FriendFutureStub friendService;
    private final @NotNull ServerDiscoveryGrpc.ServerDiscoveryFutureStub serverDiscoveryService;

    private final SDKGrpc.SDKFutureStub agonesService;
    private final SDKGrpc.SDKStub standardAgonesService;
    private final dev.agones.sdk.beta.SDKGrpc.SDKFutureStub betaAgonesService;
    private final dev.agones.sdk.alpha.SDKGrpc.SDKFutureStub alphaAgonesService;

    public GrpcStubManager() {
        ManagedChannel playerTrackerChannel = ManagedChannelBuilder.forAddress("player-tracker.towerdefence.svc", 9090)
                .defaultLoadBalancingPolicy("round_robin")
                .usePlaintext()
                .build();
        this.playerTrackerService = PlayerTrackerGrpc.newFutureStub(playerTrackerChannel);

        ManagedChannel mcPlayerChannel = ManagedChannelBuilder.forAddress("mc-player.towerdefence.svc", 9090)
                .defaultLoadBalancingPolicy("round_robin")
                .usePlaintext()
                .build();
        this.mcPlayerService = McPlayerGrpc.newFutureStub(mcPlayerChannel);

        ManagedChannel friendChannel = ManagedChannelBuilder.forAddress("friend-manager.towerdefence.svc", 9090)
                .defaultLoadBalancingPolicy("round_robin")
                .usePlaintext()
                .build();
        this.friendService = FriendGrpc.newFutureStub(friendChannel);

        ManagedChannel serverDiscoveryChannel = ManagedChannelBuilder.forAddress("server-discovery.towerdefence.svc", 9090)
                .defaultLoadBalancingPolicy("round_robin")
                .usePlaintext()
                .build();
        this.serverDiscoveryService = ServerDiscoveryGrpc.newFutureStub(serverDiscoveryChannel);

        ManagedChannel agonesChannel = ManagedChannelBuilder.forAddress("localhost", AGONES_GRPC_PORT)
                .usePlaintext()
                .build();
        this.agonesService = SDKGrpc.newFutureStub(agonesChannel);
        this.standardAgonesService = SDKGrpc.newStub(agonesChannel);
        this.betaAgonesService = dev.agones.sdk.beta.SDKGrpc.newFutureStub(agonesChannel);
        this.alphaAgonesService = dev.agones.sdk.alpha.SDKGrpc.newFutureStub(agonesChannel);
    }

    public @NotNull PlayerTrackerGrpc.PlayerTrackerFutureStub getPlayerTrackerService() {
        return playerTrackerService;
    }

    public @NotNull McPlayerGrpc.McPlayerFutureStub getMcPlayerService() {
        return mcPlayerService;
    }

    public @NotNull FriendGrpc.FriendFutureStub getFriendService() {
        return friendService;
    }

    public @NotNull ServerDiscoveryGrpc.ServerDiscoveryFutureStub getServerDiscoveryService() {
        return serverDiscoveryService;
    }

    public SDKGrpc.SDKFutureStub getAgonesService() {
        return agonesService;
    }

    public SDKGrpc.SDKStub getStandardAgonesService() {
        return standardAgonesService;
    }

    public dev.agones.sdk.beta.SDKGrpc.SDKFutureStub getBetaAgonesService() {
        return betaAgonesService;
    }

    public dev.agones.sdk.alpha.SDKGrpc.SDKFutureStub getAlphaAgonesService() {
        return alphaAgonesService;
    }
}
