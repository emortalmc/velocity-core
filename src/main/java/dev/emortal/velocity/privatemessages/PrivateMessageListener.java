package dev.emortal.velocity.privatemessages;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.message.messagehandler.PrivateMessageCreatedMessage;
import dev.emortal.api.model.messagehandler.PrivateMessage;
import dev.emortal.velocity.messaging.MessagingCore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PrivateMessageListener {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String PRIVATE_MESSAGE_RECEIVED_FORMAT = "<dark_purple>(<light_purple><username> -> You<dark_purple>) <light_purple><message>";


    public PrivateMessageListener(@NotNull ProxyServer proxy, @NotNull MessagingCore messaging, LastMessageCache lastMessageCache) {
        messaging.addListener(PrivateMessageCreatedMessage.class, message -> {
            PrivateMessage privateMessage = message.getPrivateMessage();

            proxy.getPlayer(UUID.fromString(privateMessage.getRecipientId())).ifPresent(player -> {
                player.sendMessage(MINI_MESSAGE.deserialize(PRIVATE_MESSAGE_RECEIVED_FORMAT,
                        Placeholder.parsed("username", privateMessage.getSenderUsername()),
                        Placeholder.unparsed("message", privateMessage.getMessage())
                ));

                lastMessageCache.setLastMessage(player.getUniqueId(), privateMessage.getSenderUsername());
                lastMessageCache.setLastMessage(UUID.fromString(privateMessage.getSenderId()), player.getUsername());
            });

        });
    }
}
