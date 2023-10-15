package dev.emortal.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.modules.LoadableModule;
import dev.emortal.api.modules.ModuleManager;
import dev.emortal.velocity.agones.AgonesModule;
import dev.emortal.velocity.liveconfig.KubernetesModule;
import dev.emortal.velocity.liveconfig.LiveConfigModule;
import dev.emortal.velocity.matchmaking.MatchmakerModule;
import dev.emortal.velocity.messaging.MessagingModule;
import dev.emortal.velocity.misc.CoreListenersModule;
import dev.emortal.velocity.module.VelocityModule;
import dev.emortal.velocity.module.VelocityModuleEnvironment;
import dev.emortal.velocity.module.VelocityModuleEnvironmentProvider;
import dev.emortal.velocity.monitoring.MonitoringModule;
import dev.emortal.velocity.party.PartyModule;
import dev.emortal.velocity.permissions.PermissionModule;
import dev.emortal.velocity.player.PlayerModule;
import dev.emortal.velocity.privatemessages.PrivateMessageModule;
import dev.emortal.velocity.relationships.RelationshipsModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Plugin(id = "core", name = "Core")
public final class CorePlugin {

    private final @NotNull ProxyServer proxy;

    private @Nullable ModuleManager moduleManager;

    @Inject
    public CorePlugin(@NotNull ProxyServer server) {
        this.proxy = server;
    }

    @Subscribe
    public void onProxyInitialize(@NotNull ProxyInitializeEvent event) {
        this.moduleManager = ModuleManager.builder()
                .environmentProvider(new VelocityModuleEnvironmentProvider(this.proxy, this))
                .module(AgonesModule.class, velocityModule(AgonesModule::new))
                .module(CoreListenersModule.class, velocityModule(CoreListenersModule::new))
                .module(KubernetesModule.class, KubernetesModule::new)
                .module(LiveConfigModule.class, LiveConfigModule::new)
                .module(MatchmakerModule.class, velocityModule(MatchmakerModule::new))
                .module(MessagingModule.class, velocityModule(MessagingModule::new))
                .module(PartyModule.class, velocityModule(PartyModule::new))
                .module(PermissionModule.class, velocityModule(PermissionModule::new))
                .module(PlayerModule.class, velocityModule(PlayerModule::new))
                .module(PrivateMessageModule.class, velocityModule(PrivateMessageModule::new))
                .module(RelationshipsModule.class, velocityModule(RelationshipsModule::new))
                .module(PyroscopeModule.class, PyroscopeModule::new)
                .module(MonitoringModule.class, velocityModule(MonitoringModule::new))
                .module(PacketDebuggingModule.class, velocityModule(PacketDebuggingModule::new))
                .build();

        this.moduleManager.onReady();
    }

    private static @NotNull LoadableModule.Creator velocityModule(@NotNull VelocityModule.Creator creator) {
        return environment -> creator.create((VelocityModuleEnvironment) environment);
    }

    @Subscribe
    public void onProxyShutdown(@NotNull ProxyShutdownEvent event) {
        if (this.moduleManager == null) return;

        try {
            this.moduleManager.onUnload();
        } catch (Exception ignored) {
            // do nothing
        }
    }
}
