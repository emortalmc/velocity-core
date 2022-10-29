package cc.towerdefence.velocity.listener;

import cc.towerdefence.velocity.api.event.PrivateMessageReceivedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public class PrivateMessageListener {
    private static final String RECEIVED_MESSAGE_FORMAT = "<dark_green>(<green><sender> <dark_green>-><green> you<dark_green>) <green><message>";
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final ProxyServer server;

    public PrivateMessageListener(ProxyServer server) {
        this.server = server;
    }

    @Subscribe
    public void onMessageReceived(PrivateMessageReceivedEvent event) {
        this.server.getPlayer(event.receiverId()).ifPresent(player -> {
            player.sendMessage(MINI_MESSAGE.deserialize(RECEIVED_MESSAGE_FORMAT, TagResolver.builder()
                    .tag("sender", Tag.inserting(Component.text(event.senderUsername())))
                    .tag("message", Tag.inserting(Component.text(event.message()))).build()
            ));
        });
    }

}
