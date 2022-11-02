package cc.towerdefence.velocity.tablist;

import cc.towerdefence.velocity.CorePlugin;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TabList {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final Component TAB_LIST_FOOTER = MiniMessage.miniMessage().deserialize("<light_purple><bold>ඞ                                   <dark_purple>ඞ");
    private static final String TAB_LIST_HEADER = """
            <dark_purple><bold>ඞ                                   <light_purple>ඞ
            <reset><gradient:#e2c2ff:#c98fff:<rotation>>towerdefence.cc</gradient>""";

    private final AtomicInteger atomicInteger = new AtomicInteger();
    private final ProxyServer proxy;

    public TabList(CorePlugin plugin, ProxyServer proxy) {
        this.proxy = proxy;

        this.proxy.getScheduler().buildTask(plugin, this::updateTabListHeader)
                .repeat(100, TimeUnit.MILLISECONDS)
                .schedule();
    }

    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        event.getPlayer().sendPlayerListFooter(TAB_LIST_FOOTER);
    }

    private void updateTabListHeader() {
        Component content = MINI_MESSAGE.deserialize(TAB_LIST_HEADER, Placeholder.parsed("rotation", String.valueOf(this.getGradientOffset())));

        this.proxy.getAllPlayers().forEach(player -> player.sendPlayerListHeader(content));
    }

    private double getGradientOffset() {
        return this.atomicInteger.getAndUpdate(current -> {
            if (current >= 10) return -10;
            return ++current;
        }) * 0.1;
    }
}
