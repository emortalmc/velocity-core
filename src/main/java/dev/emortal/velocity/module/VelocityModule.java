package dev.emortal.velocity.module;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.modules.Module;
import dev.emortal.velocity.player.provider.PlayerProvider;
import org.jetbrains.annotations.NotNull;

public abstract class VelocityModule extends Module {

    public VelocityModule(@NotNull VelocityModuleEnvironment environment) {
        super(environment);
    }

    protected final VelocityModuleEnvironment getEnvironment() {
        return (VelocityModuleEnvironment) this.environment;
    }

    protected final @NotNull ProxyServer getProxy() {
        return this.getEnvironment().proxy();
    }

    protected final @NotNull PlayerProvider getPlayerProvider() {
        return this.getEnvironment().playerProvider();
    }

    protected final void registerEventListener(@NotNull Object listener) {
        VelocityModuleEnvironment env = this.getEnvironment();
        env.proxy().getEventManager().register(env.plugin(), listener);
    }

    @FunctionalInterface
    public interface Creator {

        @NotNull VelocityModule create(@NotNull VelocityModuleEnvironment environment);
    }
}
