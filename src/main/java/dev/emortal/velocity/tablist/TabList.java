package dev.emortal.velocity.tablist;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.service.playertracker.PlayerTrackerService;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.velocity.CorePlugin;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public final class TabList {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Logger LOGGER = LoggerFactory.getLogger(TabList.class);

    private static final Component TAB_LIST_HEADER = Component.text()
            .append(Component.text("┌                                                  ", NamedTextColor.GOLD))
            .append(Component.text("┐ \n ", NamedTextColor.LIGHT_PURPLE))
            .append(MINI_MESSAGE.deserialize("<gradient:gold:light_purple><bold>EmortalMC"))
            .build();

    private final ProxyServer proxy;

    private Component currentFooter = Component.empty();

    public TabList(@NotNull CorePlugin plugin, @NotNull ProxyServer proxy) {
        this.proxy = proxy;

        PlayerTrackerService playerTracker = GrpcStubCollection.getPlayerTrackerService().orElse(null);
        if (playerTracker == null) {
            this.currentFooter = this.createFooter(0);
        } else {
            this.proxy.getScheduler().buildTask(plugin, () -> this.updateFooter(playerTracker))
                    .repeat(5, TimeUnit.SECONDS)
                    .schedule();
        }
    }

    @Subscribe
    public void onPlayerJoin(@NotNull PostLoginEvent event) {
        event.getPlayer().sendPlayerListHeaderAndFooter(TAB_LIST_HEADER, this.currentFooter);
    }

    private void updateFooter(@NotNull PlayerTrackerService playerTracker) {
        long playerCount;
        try {
            playerCount = playerTracker.getGlobalPlayerCount();
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to update tab list footer: ", exception);
            return;
        }

        this.currentFooter = this.createFooter(playerCount);
        this.updateOnlinePlayers();
    }

    private void updateOnlinePlayers() {
        for (Player player : this.proxy.getAllPlayers()) {
            player.sendPlayerListFooter(this.currentFooter);
        }
    }

    private @NotNull Component createFooter(long playerCount) {
        return Component.text()
                .append(Component.text(" \n" + playerCount + " online", NamedTextColor.GRAY))
                .append(Component.text("\nᴍᴄ.ᴇᴍᴏʀᴛᴀʟ.ᴅᴇᴠ", TextColor.color(38, 110, 224)))
                .append(Component.text("\n└                                                  ", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text("┘ ", NamedTextColor.GOLD))
                .build();
    }
}
