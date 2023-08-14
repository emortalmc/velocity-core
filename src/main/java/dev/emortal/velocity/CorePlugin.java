package dev.emortal.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.modules.ModuleManager;
import dev.emortal.api.utils.resolvers.PlayerResolver;
import dev.emortal.velocity.agones.AgonesModule;
import dev.emortal.velocity.command.CommandModule;
import dev.emortal.velocity.listener.LunarKicker;
import dev.emortal.velocity.liveconfig.KubernetesModule;
import dev.emortal.velocity.liveconfig.LiveConfigModule;
import dev.emortal.velocity.matchmaking.MatchmakerModule;
import dev.emortal.velocity.messaging.MessagingModule;
import dev.emortal.velocity.module.ModuleManagerBuilder;
import dev.emortal.velocity.party.PartyModule;
import dev.emortal.velocity.permissions.PermissionModule;
import dev.emortal.velocity.player.PlayerServiceModule;
import dev.emortal.velocity.privatemessages.PrivateMessageModule;
import dev.emortal.velocity.relationships.RelationshipsModule;
import dev.emortal.velocity.resourcepack.ResourcePackForcer;
import dev.emortal.velocity.serverlist.ServerPingListener;
import dev.emortal.velocity.tablist.TabList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Plugin(
        id = "core",
        name = "Core"
)
public final class CorePlugin {

    private final ProxyServer proxy;
    private @Nullable ModuleManager moduleManager;

    @Inject
    public CorePlugin(@NotNull ProxyServer server) {
        this.proxy = server;
    }

    @Subscribe
    public void onProxyInitialize(@NotNull ProxyInitializeEvent event) {
        PlayerResolver.setPlatformUsernameResolver(username -> this.proxy.getPlayer(username)
                .map(player -> new PlayerResolver.CachedMcPlayer(player.getUniqueId(), player.getUsername(), true))
                .orElse(null));

        this.moduleManager = ModuleManagerBuilder.create()
                .module(AgonesModule.class, AgonesModule::new)
                .module(CommandModule.class, CommandModule::new)
                .module(KubernetesModule.class, KubernetesModule::new)
                .module(LiveConfigModule.class, LiveConfigModule::new)
                .module(MatchmakerModule.class, MatchmakerModule::new)
                .module(MessagingModule.class, MessagingModule::new)
                .module(PartyModule.class, PartyModule::new)
                .module(PermissionModule.class, PermissionModule::new)
                .module(PlayerServiceModule.class, PlayerServiceModule::new)
                .module(PrivateMessageModule.class, PrivateMessageModule::new)
                .module(RelationshipsModule.class, RelationshipsModule::new)
                .module(PyroscopeModule.class, PyroscopeModule::new)
                .module(PacketDebuggingModule.class, PacketDebuggingModule::new)
                .build(this, this.proxy);

        this.moduleManager.onReady();

        EventManager eventManager = this.proxy.getEventManager();

        // server list
        eventManager.register(this, new ServerPingListener());

        // tablist
        eventManager.register(this, new TabList(this, this.proxy));

        // resource pack
        eventManager.register(this, new ResourcePackForcer(this.proxy));

        // fuck lunar
        eventManager.register(this, new LunarKicker());

        // server cleanup
        eventManager.register(this, new ServerCleanupTask(this.proxy));
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
