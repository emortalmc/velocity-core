package dev.emortal.velocity.adapter;

import dev.emortal.velocity.adapter.command.EmortalCommandManager;
import dev.emortal.velocity.adapter.event.EmortalEventManager;
import dev.emortal.velocity.adapter.player.PlayerProvider;
import dev.emortal.velocity.adapter.resourcepack.ResourcePackProvider;
import dev.emortal.velocity.adapter.scheduler.EmortalScheduler;
import dev.emortal.velocity.adapter.server.ServerProvider;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;

public record AdapterContext(@NotNull EmortalCommandManager commandManager, @NotNull EmortalEventManager eventManager,
                             @NotNull PlayerProvider playerProvider, @NotNull EmortalScheduler scheduler,
                             @NotNull ServerProvider serverProvider, @NotNull ResourcePackProvider resourcePackProvider,
                             @NotNull Audience audience) {
}
