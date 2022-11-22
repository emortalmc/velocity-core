package cc.towerdefence.velocity.tablist;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class TabList {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final Component TAB_LIST_FOOTER = MINI_MESSAGE.deserialize("<light_purple><bold>ඞ                                   <dark_purple>ඞ");
    private static final Component TAB_LIST_HEADER = MINI_MESSAGE.deserialize("""
            <dark_purple><bold>ඞ                                   <light_purple>ඞ
            <reset><gradient:#e2c2ff:#c98fff>towerdefence.cc</gradient>""");

    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        event.getPlayer().sendPlayerListHeaderAndFooter(TAB_LIST_HEADER, TAB_LIST_FOOTER);
    }

}
