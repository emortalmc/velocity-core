package dev.emortal.velocity.grpc.stub;

import dev.agones.sdk.SDKGrpc;
import dev.emortal.velocity.Environment;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.jetbrains.annotations.Nullable;

public class GrpcStubManager {
    private static final boolean AGONES_SDK_ENABLED;
    private static final String AGONES_ADDRESS = "localhost"; // SDK runs as a sidecar in production so address is always localhost
    private static final int AGONES_GRPC_PORT;

    static {
        String agonesPortString = System.getenv("AGONES_SDK_GRPC_PORT");
        AGONES_SDK_ENABLED = Environment.isProduction() || agonesPortString != null;

        AGONES_GRPC_PORT = AGONES_SDK_ENABLED ? Integer.parseInt(agonesPortString) : Integer.MIN_VALUE;
    }

    private final @Nullable SDKGrpc.SDKFutureStub agonesService;
    private final @Nullable SDKGrpc.SDKStub standardAgonesService;
    private final @Nullable dev.agones.sdk.beta.SDKGrpc.SDKFutureStub betaAgonesService;
    private final @Nullable dev.agones.sdk.alpha.SDKGrpc.SDKFutureStub alphaAgonesService;

    public GrpcStubManager() {
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

        this.agonesService = SDKGrpc.newFutureStub(agonesChannel);
        this.standardAgonesService = SDKGrpc.newStub(agonesChannel);
        this.betaAgonesService = dev.agones.sdk.beta.SDKGrpc.newFutureStub(agonesChannel);
        this.alphaAgonesService = dev.agones.sdk.alpha.SDKGrpc.newFutureStub(agonesChannel);
    }

    public @Nullable SDKGrpc.SDKFutureStub getAgonesService() {
        return agonesService;
    }

    public @Nullable SDKGrpc.SDKStub getStandardAgonesService() {
        return standardAgonesService;
    }

    public @Nullable dev.agones.sdk.beta.SDKGrpc.SDKFutureStub getBetaAgonesService() {
        return betaAgonesService;
    }

    public @Nullable dev.agones.sdk.alpha.SDKGrpc.SDKFutureStub getAlphaAgonesService() {
        return alphaAgonesService;
    }
}
