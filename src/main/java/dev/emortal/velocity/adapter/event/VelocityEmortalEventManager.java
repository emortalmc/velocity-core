package dev.emortal.velocity.adapter.event;

import com.velocitypowered.api.event.EventManager;
import dev.emortal.velocity.CorePlugin;
import org.jetbrains.annotations.NotNull;

public final class VelocityEmortalEventManager implements EmortalEventManager {

    private final EventManager eventManager;
    private final CorePlugin plugin;

    public VelocityEmortalEventManager(@NotNull EventManager eventManager, @NotNull CorePlugin plugin) {
        this.eventManager = eventManager;
        this.plugin = plugin;
    }

    @Override
    public void register(@NotNull Object listener) {
        this.eventManager.register(this.plugin, listener);
    }
}
