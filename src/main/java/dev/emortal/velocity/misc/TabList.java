package dev.emortal.velocity.misc;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.service.playertracker.PlayerTrackerService;
import dev.emortal.velocity.adapter.player.PlayerProvider;
import dev.emortal.velocity.adapter.scheduler.EmortalScheduler;
import dev.emortal.velocity.lang.ChatMessages;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public final class TabList {
    private static final Logger LOGGER = LoggerFactory.getLogger(TabList.class);

    private final @NotNull PlayerProvider playerProvider;

    private @NotNull Component currentFooter = Component.empty();

    public TabList(@NotNull EmortalScheduler scheduler, @NotNull PlayerProvider playerProvider, @Nullable PlayerTrackerService playerTracker) {
        this.playerProvider = playerProvider;

        if (playerTracker == null) {
            this.currentFooter = this.createFooter(this.playerProvider.playerCount());
        } else {
            scheduler.repeat(() -> this.updateFooter(playerTracker), 5, TimeUnit.SECONDS);
        }
    }

    @Subscribe
    public void onPlayerJoin(@NotNull PostLoginEvent event) {
        event.getPlayer().sendPlayerListHeaderAndFooter(ChatMessages.TAB_LIST_HEADER.parse(), this.currentFooter);
    }

    private void updateFooter(@NotNull PlayerTrackerService playerTracker) {
        long playerCount;
        try {
            playerCount = playerTracker.getGlobalPlayerCount();
        } catch (StatusRuntimeException exception) {
            LOGGER.warn("Failed to get global player count to update tab list footer", exception);
            playerCount = this.playerProvider.playerCount();
        }

        this.currentFooter = this.createFooter(playerCount);
        this.updateOnlinePlayers();
    }

    private void updateOnlinePlayers() {
        for (Player player : this.playerProvider.allPlayers()) {
            player.sendPlayerListFooter(this.currentFooter);
        }
    }

    private @NotNull Component createFooter(long playerCount) {
        return ChatMessages.TAB_LIST_FOOTER.parse(Component.text(playerCount));
    }
}
