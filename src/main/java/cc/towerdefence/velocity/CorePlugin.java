package cc.towerdefence.velocity;

import cc.towerdefence.velocity.cache.FriendCache;
import cc.towerdefence.velocity.cache.SessionCache;
import cc.towerdefence.velocity.grpc.service.GrpcServerContainer;
import cc.towerdefence.velocity.grpc.stub.GrpcStubManager;
import cc.towerdefence.velocity.listener.AgonesListener;
import cc.towerdefence.velocity.listener.LobbySelectorListener;
import cc.towerdefence.velocity.listener.McPlayerListener;
import cc.towerdefence.velocity.listener.PlayerTrackerListener;
import cc.towerdefence.velocity.listener.PrivateMessageListener;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;

@Plugin(
        id = "core",
        name = "Core"
)
public class CorePlugin {
    public static final String SERVER_ID = System.getenv("HOSTNAME");
    public static final boolean DEV_ENVIRONMENT = System.getenv("AGONES_SDK_GRPC_PORT") == null;

    private final ProxyServer proxy;

    private final GrpcStubManager stubManager = new GrpcStubManager();
    private final GrpcServerContainer grpcServerContainer;

    private final FriendCache friendCache = new FriendCache();
    private final SessionCache sessionCache = new SessionCache();

    @Inject
    public CorePlugin(ProxyServer server) {
        this.proxy = server;
        this.grpcServerContainer = new GrpcServerContainer(this.proxy);
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        this.proxy.getEventManager().register(this, new AgonesListener(this.stubManager.getAgonesService(),
                this.stubManager.getStandardAgonesService(), this.stubManager.getAlphaAgonesService())
        );
        this.proxy.getEventManager().register(this, new LobbySelectorListener(this.stubManager.getServerDiscoveryService(), this.proxy));
        this.proxy.getEventManager().register(this, new McPlayerListener(this.stubManager.getMcPlayerService(), this.sessionCache));
        this.proxy.getEventManager().register(this, new PlayerTrackerListener(this.stubManager.getPlayerTrackerService()));
        this.proxy.getEventManager().register(this, new PrivateMessageListener(this.proxy));
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        this.grpcServerContainer.stop();
    }

    public ProxyServer getProxy() {
        return this.proxy;
    }

    public GrpcStubManager getStubManager() {
        return this.stubManager;
    }
}
