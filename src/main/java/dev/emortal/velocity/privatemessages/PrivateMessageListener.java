package dev.emortal.velocity.privatemessages;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.message.messagehandler.PrivateMessageCreatedMessage;
import dev.emortal.api.model.messagehandler.PrivateMessage;
import dev.emortal.velocity.messaging.MessagingModule;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class PrivateMessageListener {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String PRIVATE_MESSAGE_RECEIVED_FORMAT = "<dark_purple>(<light_purple><username> -> You<dark_purple>) <light_purple><message>";


    private final ProxyServer proxy;
    private final LastMessageCache lastMessageCache;

    public PrivateMessageListener(@NotNull ProxyServer proxy, @NotNull MessagingModule messaging, LastMessageCache lastMessageCache) {
        this.proxy = proxy;
        this.lastMessageCache = lastMessageCache;

        messaging.addListener(PrivateMessageCreatedMessage.class, message -> this.onPrivateMessageCreated(message.getPrivateMessage()));
    }

    private void onPrivateMessageCreated(@NotNull PrivateMessage message) {
        UUID senderId = UUID.fromString(message.getSenderId());
        if (this.proxy.getPlayer(senderId).isPresent()) {
            // only update the last message cache for the sender if they are on this proxy
            this.lastMessageCache.setLastMessage(senderId, message.getRecipientUsername());
        }

        UUID recipientId = UUID.fromString(message.getRecipientId());
        Player recipient = this.proxy.getPlayer(recipientId).orElse(null);
        if (recipient == null) return;

        recipient.sendMessage(MINI_MESSAGE.deserialize(PRIVATE_MESSAGE_RECEIVED_FORMAT,
                Placeholder.parsed("username", message.getSenderUsername()),
                Placeholder.unparsed("message", message.getMessage())));

        // only update the message cache for the recipient if they are on this proxy
        this.lastMessageCache.setLastMessage(recipientId, message.getSenderUsername());
    }
}
