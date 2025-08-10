package dev.emortal.velocity.misc.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import dev.emortal.velocity.lang.ChatMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

public class ServerPingListener {

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
            "This server is certified aladeen!",
    };

    @Subscribe
    void onServerPing(@NotNull ProxyPingEvent event) {
        ServerPing ping = event.getPing().asBuilder().description(createMessage()).build();
        event.setPing(ping);
    }

    private @NotNull Component createMessage() {
        String randomMessage = this.selectRandomMessage();
        return Component.text()
                .append(ChatMessages.PING_MOTD.get())
                .appendNewline()
                .append(Component.text(randomMessage, NamedTextColor.YELLOW))
                .build();
    }

    private @NotNull String selectRandomMessage() {
        return MOTDS[ThreadLocalRandom.current().nextInt(MOTDS.length)];
    }

}
