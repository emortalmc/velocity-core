package dev.emortal.velocity.module;

import dev.emortal.api.modules.Module;
import dev.emortal.velocity.adapter.AdapterContext;
import dev.emortal.velocity.command.EmortalCommand;
import org.jetbrains.annotations.NotNull;

public abstract class VelocityModule extends Module {

    public VelocityModule(@NotNull VelocityModuleEnvironment environment) {
        super(environment);
    }

    private @NotNull VelocityModuleEnvironment getEnvironment() {
        return (VelocityModuleEnvironment) this.environment;
    }

    protected final @NotNull AdapterContext adapters() {
        return this.getEnvironment().adapters();
    }

    protected final void registerCommand(@NotNull EmortalCommand command) {
        this.adapters().commandManager().registerCommand(command);
    }

    protected final void registerEventListener(@NotNull Object listener) {
        this.adapters().eventManager().register(listener);
    }

    @FunctionalInterface
    public interface Creator {

        @NotNull VelocityModule create(@NotNull VelocityModuleEnvironment environment);
    }
}
