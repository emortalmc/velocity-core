package dev.emortal.velocity.module;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.modules.ModuleProvider;
import dev.emortal.api.modules.annotation.ModuleData;
import dev.emortal.api.modules.env.ModuleEnvironment;
import dev.emortal.api.modules.extension.ModuleEnvironmentProvider;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.velocity.CorePlugin;
import dev.emortal.velocity.adapter.AdapterContext;
import dev.emortal.velocity.adapter.command.VelocityEmortalCommandManager;
import dev.emortal.velocity.adapter.event.VelocityEmortalEventManager;
import dev.emortal.velocity.adapter.player.VelocityPlayerProvider;
import dev.emortal.velocity.adapter.resourcepack.VelocityResourcePackProvider;
import dev.emortal.velocity.adapter.scheduler.VelocityEmortalScheduler;
import dev.emortal.velocity.adapter.server.VelocityServerProvider;
import dev.emortal.velocity.player.resolver.PlayerResolver;
import dev.emortal.velocity.player.suggestions.BasicUsernameSuggesterProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class VelocityModuleEnvironmentProvider implements ModuleEnvironmentProvider {

    private final @NotNull AdapterContext adapterContext;
    private final @Nullable McPlayerService playerService;
    private final @NotNull PlayerResolver playerResolver;

    public VelocityModuleEnvironmentProvider(@NotNull ProxyServer proxy, @NotNull CorePlugin plugin) {
        this.adapterContext = this.createAdapterContext(proxy, plugin);
        this.playerService = GrpcStubCollection.getPlayerService().orElse(null);
        this.playerResolver = new PlayerResolver(this.playerService, this.adapterContext.playerProvider());
    }

    private @NotNull AdapterContext createAdapterContext(@NotNull ProxyServer proxy, @NotNull CorePlugin plugin) {
        return new AdapterContext(
                new VelocityEmortalCommandManager(proxy.getCommandManager(), plugin, new BasicUsernameSuggesterProvider(this.playerService)),
                new VelocityEmortalEventManager(proxy.getEventManager(), plugin),
                new VelocityPlayerProvider(proxy),
                new VelocityEmortalScheduler(proxy.getScheduler(), plugin),
                new VelocityServerProvider(proxy),
                new VelocityResourcePackProvider(proxy)
        );
    }

    @Override
    public @NotNull ModuleEnvironment create(@NotNull ModuleData data, @NotNull ModuleProvider provider) {
        return new VelocityModuleEnvironment(data, provider, this.adapterContext, this.playerService, this.playerResolver);
    }
}
