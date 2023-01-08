package dev.emortal.velocity.privatemessages;

import dev.emortal.velocity.api.event.PrivateMessageReceivedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public class PrivateMessageListener {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String PRIVATE_MESSAGE_RECEIVED_FORMAT = "<dark_purple>(<light_purple><username> -> You<dark_purple>) <light_purple><message>";

    private final ProxyServer proxy;
    private final LastMessageCache lastMessageCache;

    public PrivateMessageListener(ProxyServer proxy, LastMessageCache lastMessageCache) {
        this.proxy = proxy;
        this.lastMessageCache = lastMessageCache;
    }

    @Subscribe
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        this.proxy.getPlayer(event.receiverId()).ifPresent(player -> {
            player.sendMessage(MINI_MESSAGE.deserialize(PRIVATE_MESSAGE_RECEIVED_FORMAT,
                    Placeholder.parsed("username", event.senderUsername()),
                    Placeholder.unparsed("message", event.message())
            ));

            this.lastMessageCache.setLastMessage(player.getUniqueId(), event.senderUsername());
        });
    }
}
