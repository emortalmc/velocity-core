package dev.emortal.velocity.privatemessages;

import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.message.messagehandler.PrivateMessageCreatedMessage;
import dev.emortal.api.model.messagehandler.PrivateMessage;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.messaging.MessagingModule;
import dev.emortal.velocity.adapter.player.PlayerProvider;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class PrivateMessageListener {

    private final @NotNull PlayerProvider playerProvider;
    private final @NotNull LastMessageCache lastMessageCache;

    public PrivateMessageListener(@NotNull PlayerProvider playerProvider, @NotNull MessagingModule messaging,
                                  @NotNull LastMessageCache lastMessageCache) {
        this.playerProvider = playerProvider;
        this.lastMessageCache = lastMessageCache;

        messaging.addListener(PrivateMessageCreatedMessage.class, message -> this.onPrivateMessageCreated(message.getPrivateMessage()));
    }

    private void onPrivateMessageCreated(@NotNull PrivateMessage message) {
        UUID senderId = UUID.fromString(message.getSenderId());
        if (this.playerProvider.getPlayer(senderId) != null) {
            // only update the last message cache for the sender if they are on this proxy
            this.lastMessageCache.setLastMessage(senderId, message.getRecipientUsername());
        }

        UUID recipientId = UUID.fromString(message.getRecipientId());
        Player recipient = this.playerProvider.getPlayer(recipientId);
        if (recipient == null) return;

        ChatMessages.PRIVATE_MESSAGE_RECEIVED.send(recipient, Component.text(message.getSenderUsername()), Component.text(message.getMessage()));

        // only update the message cache for the recipient if they are on this proxy
        this.lastMessageCache.setLastMessage(recipientId, message.getSenderUsername());
    }
}
