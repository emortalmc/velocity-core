package dev.emortal.velocity.agones;

import dev.agones.sdk.SDKGrpc;
import dev.emortal.velocity.Environment;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.jetbrains.annotations.Nullable;

final class AgonesGrpcStubCollection {
    private static final boolean AGONES_SDK_ENABLED;
    private static final String AGONES_ADDRESS = "localhost"; // SDK runs as a sidecar in production so address is always localhost
    private static final int AGONES_GRPC_PORT;

    static {
        String agonesPortString = System.getenv("AGONES_SDK_GRPC_PORT");
        AGONES_SDK_ENABLED = Environment.isKubernetes() || agonesPortString != null;
        AGONES_GRPC_PORT = AGONES_SDK_ENABLED ? Integer.parseInt(agonesPortString) : Integer.MIN_VALUE;
    }

    private final @Nullable SDKGrpc.SDKBlockingStub agonesService;
    private final @Nullable SDKGrpc.SDKStub standardAgonesService;
    private final @Nullable dev.agones.sdk.beta.SDKGrpc.SDKBlockingStub betaAgonesService;
    private final @Nullable dev.agones.sdk.alpha.SDKGrpc.SDKBlockingStub alphaAgonesService;

    AgonesGrpcStubCollection() {
        if (!AGONES_SDK_ENABLED) {
            this.agonesService = null;
            this.standardAgonesService = null;
            this.betaAgonesService = null;
            this.alphaAgonesService = null;
            return;
        }

        ManagedChannel agonesChannel = ManagedChannelBuilder.forAddress(AGONES_ADDRESS, AGONES_GRPC_PORT)
                .usePlaintext()
                .build();

        this.agonesService = SDKGrpc.newBlockingStub(agonesChannel);
        this.standardAgonesService = SDKGrpc.newStub(agonesChannel);
        this.betaAgonesService = dev.agones.sdk.beta.SDKGrpc.newBlockingStub(agonesChannel);
        this.alphaAgonesService = dev.agones.sdk.alpha.SDKGrpc.newBlockingStub(agonesChannel);
    }

    @Nullable SDKGrpc.SDKBlockingStub getAgonesService() {
        return this.agonesService;
    }

    @Nullable SDKGrpc.SDKStub getStandardAgonesService() {
        return this.standardAgonesService;
    }

    @Nullable dev.agones.sdk.beta.SDKGrpc.SDKBlockingStub getBetaAgonesService() {
        return this.betaAgonesService;
    }

    @Nullable dev.agones.sdk.alpha.SDKGrpc.SDKBlockingStub getAlphaAgonesService() {
        return this.alphaAgonesService;
    }
}
