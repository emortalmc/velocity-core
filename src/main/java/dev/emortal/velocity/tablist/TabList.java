package dev.emortal.velocity.tablist;

import dev.emortal.velocity.CorePlugin;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.model.ServerType;
import dev.emortal.api.service.PlayerTrackerGrpc;
import dev.emortal.api.service.PlayerTrackerProto;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class TabList {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Logger LOGGER = LoggerFactory.getLogger(TabList.class);

    private static final Component TAB_LIST_HEADER = MINI_MESSAGE.deserialize("<light_purple><bold>ඞ                                   <dark_purple>ඞ</bold>");
    private static final String TAB_LIST_FOOTER = """
                        
            <gray><online_players> online</gray>
            <reset><gradient:#e2c2ff:#c98fff>towerdefence.cc</gradient>
            <dark_purple><bold>ඞ                                   <light_purple>ඞ</bold>""";

    private final PlayerTrackerGrpc.PlayerTrackerFutureStub playerTracker;
    private final ProxyServer proxy;

    private Component currentFooter = Component.empty();

    public TabList(CorePlugin plugin, ProxyServer proxy) {
        this.proxy = proxy;

        this.playerTracker = GrpcStubCollection.getPlayerTrackerService().orElse(null);
        if (this.playerTracker == null) {
            this.currentFooter = MINI_MESSAGE.deserialize(TAB_LIST_FOOTER, Placeholder.parsed("online_players", "Not Connected"));
        } else {
            this.proxy.getScheduler().buildTask(plugin, this::updateFooter)
                    .repeat(5, TimeUnit.SECONDS).schedule();
        }
    }

    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        event.getPlayer().sendPlayerListHeaderAndFooter(TAB_LIST_HEADER, this.currentFooter);
    }

    private void updateFooter() {
        ListenableFuture<PlayerTrackerProto.ServerTypePlayerCountResponse> responseFuture = this.playerTracker.getServerTypePlayerCount(
                PlayerTrackerProto.ServerTypeRequest.newBuilder()
                        .setServerType(ServerType.PROXY).build());

        Futures.addCallback(responseFuture, FunctionalFutureCallback.create(
                response -> {
                    this.currentFooter = this.createFooter(response.getPlayerCount());
                    this.updateOnlinePlayers();

                },
                throwable -> LOGGER.error("Failed to update tab list footer: ", throwable)
        ), ForkJoinPool.commonPool());
    }

    private void updateOnlinePlayers() {
        for (Player player : this.proxy.getAllPlayers()) {
            player.sendPlayerListFooter(this.currentFooter);
        }
    }

    private Component createFooter(int playerCount) {
        return MINI_MESSAGE.deserialize(TAB_LIST_FOOTER, Placeholder.parsed("online_players", String.valueOf(playerCount)));
    }
}
