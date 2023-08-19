package dev.emortal.velocity.module;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.modules.LoadableModule;
import dev.emortal.api.modules.Module;
import dev.emortal.api.modules.ModuleManager;
import dev.emortal.api.modules.env.ModuleEnvironment;
import dev.emortal.velocity.CorePlugin;
import dev.emortal.velocity.player.provider.PlayerProvider;
import dev.emortal.velocity.player.provider.VelocityPlayerProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManagerBuilder {

    public static @NotNull ModuleManagerBuilder create() {
        return new ModuleManagerBuilder();
    }

    private final List<LoadableModule> modules = new ArrayList<>();

    private ModuleManagerBuilder() {
    }

    public <T extends Module> @NotNull ModuleManagerBuilder module(@NotNull Class<T> type, @NotNull LoadableModule.Creator creator) {
        return this.addModule(type, creator);
    }

    public <T extends Module> @NotNull ModuleManagerBuilder module(@NotNull Class<T> type, @NotNull VelocityModule.Creator creator) {
        return this.addModule(type, environment -> creator.create((VelocityModuleEnvironment) environment));
    }

    private <T extends Module> @NotNull ModuleManagerBuilder addModule(@NotNull Class<T> type, @NotNull LoadableModule.Creator creator) {
        this.modules.add(new LoadableModule(type, creator));
        return this;
    }

    public @NotNull ModuleManager build(@NotNull CorePlugin plugin, @NotNull ProxyServer proxy) {
        PlayerProvider playerProvider = new VelocityPlayerProvider(proxy);
        ModuleEnvironment.Provider provider = (data, moduleProvider) -> new VelocityModuleEnvironment(data, moduleProvider, plugin, proxy, playerProvider);
        return new ModuleManager(this.modules, provider);
    }
}
