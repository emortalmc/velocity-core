package cc.towerdefence.velocity.serverlist;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class ServerPingListener {
    private static final String MOTD = """
            <gradient:#c98fff:#ff63d6>                ★ TowerDefence ★
                       now with at least 2 towers""";

    @Subscribe
    public void onServerPing(ProxyPingEvent event) {
        ServerPing ping = event.getPing().asBuilder()
                        .description(
                                MiniMessage.miniMessage().deserialize(MOTD)
                        ).build();

        event.setPing(ping);
    }
}
