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
import net.kyori.adventure.text.Component;
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

    void sendMessage(@NotNull Player player, @NotNull String targetUsername, @NotNull String message) {
        CachedMcPlayer target;
        try {
            target = this.playerResolver.getPlayer(targetUsername);
        } catch (StatusException exception) {
            LOGGER.error("Failed to get player data for '{}'", targetUsername, exception);
            ChatMessages.GENERIC_ERROR.send(player);
            return;
        }

        if (target == null) {
            ChatMessages.PLAYER_NOT_FOUND.send(player, Component.text(targetUsername));
            return;
        }

        String correctedUsername = target.username();
        UUID targetId = target.uuid();

        if (!target.online()) {
            ChatMessages.PLAYER_NOT_ONLINE.send(player, Component.text(correctedUsername));
            return;
        }

        PrivateMessage privateMessage = PrivateMessage.newBuilder()
                .setSenderId(player.getUniqueId().toString())
                .setSenderUsername(player.getUsername())
                .setRecipientId(targetId.toString())
                .setMessage(message)
                .build();

        SendPrivateMessageResult result;
        try {
            result = this.messageService.sendPrivateMessage(privateMessage);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to send message from '{}' to '{}': '{}'", player.getUsername(), correctedUsername, message, exception);
            ChatMessages.GENERIC_ERROR.send(player);
            return;
        }

        switch (result) {
            case SendPrivateMessageResult.Success(PrivateMessage ignored) ->
                    ChatMessages.PRIVATE_MESSAGE_SENT.send(player, Component.text(correctedUsername), Component.text(message));
            case SendPrivateMessageResult.Error error -> {
                switch (error) {
                    case YOU_BLOCKED -> ChatMessages.ERROR_YOU_BLOCKED.send(player, Component.text(correctedUsername));
                    case PRIVACY_BLOCKED -> ChatMessages.ERROR_THEY_BLOCKED.send(player, Component.text(correctedUsername));
                    case PLAYER_NOT_ONLINE -> ChatMessages.PLAYER_NOT_ONLINE.send(player, Component.text(correctedUsername));
                }
            }
        }
    }
}
