package dev.emortal.velocity.module;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.modules.ModuleProvider;
import dev.emortal.api.modules.annotation.ModuleData;
import dev.emortal.api.modules.env.ModuleEnvironment;
import dev.emortal.api.modules.extension.ModuleEnvironmentProvider;
import dev.emortal.velocity.CorePlugin;
import dev.emortal.velocity.player.provider.PlayerProvider;
import dev.emortal.velocity.player.provider.VelocityPlayerProvider;
import org.jetbrains.annotations.NotNull;

public final class VelocityModuleEnvironmentProvider implements ModuleEnvironmentProvider {

    private final ProxyServer proxy;
    private final CorePlugin plugin;
    private final PlayerProvider playerProvider;

    public VelocityModuleEnvironmentProvider(@NotNull ProxyServer proxy, @NotNull CorePlugin plugin) {
        this.proxy = proxy;
        this.plugin = plugin;
        this.playerProvider = new VelocityPlayerProvider(proxy);
    }

    @Override
    public @NotNull ModuleEnvironment create(@NotNull ModuleData data, @NotNull ModuleProvider provider) {
        return new VelocityModuleEnvironment(data, provider, this.plugin, this.proxy, this.playerProvider);
    }
}
