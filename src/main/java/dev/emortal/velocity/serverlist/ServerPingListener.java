package dev.emortal.velocity.serverlist;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.concurrent.ThreadLocalRandom;

public class ServerPingListener {
    private static final Component MOTD = Component.text()
            .append(Component.text("▓▒░              ", NamedTextColor.LIGHT_PURPLE))
            .append(Component.text("⚡   ", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
            .append(MiniMessage.miniMessage().deserialize("<gradient:gold:light_purple><bold>EmortalMC"))
            .append(Component.text("   ⚡", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text("              ░▒▓", NamedTextColor.GOLD))
            .build();

    private static final String[] MOTDS = new String[] {
            "coolest server to ever exist",
            "better than hypixel",
            "you should join",
            "stop scrolling, click here!",
            "Lunar client users: Beware!",
            "using 3 server softwares!",
            "gradient lover",
            "emortal is watching",
            "emortal says 2 + 2 = 5",
            "Chuck Norris joined and said it was pretty good",
            "Chuck Norris doesn't join, the server joins him",
            "private lobbies when?",
            "I heard SunriseMC was releasing soon...",
    };

    @Subscribe
    public void onServerPing(ProxyPingEvent event) {
        String randomMessage = MOTDS[ThreadLocalRandom.current().nextInt(MOTDS.length)];
        ServerPing ping = event.getPing().asBuilder()
                .description(MOTD.append(Component.text("\n" + randomMessage, NamedTextColor.YELLOW)))
                .build();

        event.setPing(ping);

    }
}
