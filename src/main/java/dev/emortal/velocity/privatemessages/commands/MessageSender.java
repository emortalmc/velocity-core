package dev.emortal.velocity.privatemessages.commands;

import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.model.messagehandler.PrivateMessage;
import dev.emortal.api.service.messagehandler.MessageService;
import dev.emortal.api.service.messagehandler.SendPrivateMessageResult;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.player.resolver.CachedMcPlayer;
import dev.emortal.velocity.player.resolver.PlayerResolver;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public final class MessageSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageSender.class);

    private final @NotNull MessageService messageService;
    private final @NotNull PlayerResolver playerResolver;

    public MessageSender(@NotNull MessageService messageService, @NotNull PlayerResolver playerResolver) {
        this.messageService = messageService;
        this.playerResolver = playerResolver;
    }

    void sendMessage(@NotNull Player sender, @NotNull UUID targetId, @NotNull String message) {
        CachedMcPlayer target;
        try {
            target = this.playerResolver.getPlayer(targetId);
        } catch (StatusException exception) {
            LOGGER.error("Failed to get player data for '{}'", targetId, exception);
            ChatMessages.GENERIC_ERROR.send(sender);
            return;
        }

        if (target == null) {
            ChatMessages.GENERIC_ERROR.send(sender);
            LOGGER.error("Player data not found for '{}'", targetId);
            return;
        }

        this.sendMessage(sender, target, message);
    }

    void sendMessage(@NotNull Player sender, @NotNull String targetUsername, @NotNull String message) {
        CachedMcPlayer target;
        try {
            target = this.playerResolver.getPlayer(targetUsername);
        } catch (StatusException exception) {
            LOGGER.error("Failed to get player data for '{}'", targetUsername, exception);
            ChatMessages.GENERIC_ERROR.send(sender);
            return;
        }

        if (target == null) {
            ChatMessages.PLAYER_NOT_FOUND.send(sender, targetUsername);
            return;
        }

        this.sendMessage(sender, target, message);
    }

    void sendMessage(@NotNull Player sender, @NotNull CachedMcPlayer target, @NotNull String message) {
        String correctedUsername = target.username();
        UUID targetId = target.uuid();

        if (!target.online()) {
            ChatMessages.PLAYER_NOT_ONLINE.send(sender, correctedUsername);
            return;
        }

        PrivateMessage privateMessage = PrivateMessage.newBuilder()
                .setSenderId(sender.getUniqueId().toString())
                .setSenderUsername(sender.getUsername())
                .setRecipientId(targetId.toString())
                .setMessage(message)
                .build();

        SendPrivateMessageResult result;
        try {
            result = this.messageService.sendPrivateMessage(privateMessage);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to send message from '{}' to '{}': '{}'", sender.getUsername(), correctedUsername, message, exception);
            ChatMessages.GENERIC_ERROR.send(sender);
            return;
        }

        switch (result) {
            case SendPrivateMessageResult.Success ignored -> ChatMessages.PRIVATE_MESSAGE_SENT.send(sender, correctedUsername, message);
            case SendPrivateMessageResult.Error error -> {
                switch (error) {
                    case YOU_BLOCKED -> ChatMessages.ERROR_YOU_BLOCKED.send(sender, correctedUsername);
                    case PRIVACY_BLOCKED -> ChatMessages.ERROR_THEY_BLOCKED.send(sender, correctedUsername);
                    case PLAYER_NOT_ONLINE -> ChatMessages.PLAYER_NOT_ONLINE.send(sender, correctedUsername);
                }
            }
        }
    }
}
