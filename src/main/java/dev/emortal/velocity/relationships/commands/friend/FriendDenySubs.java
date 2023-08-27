package dev.emortal.velocity.relationships.commands.friend;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.grpc.relationship.RelationshipProto;
import dev.emortal.api.service.relationship.RelationshipService;
import dev.emortal.api.utils.resolvers.PlayerResolver;
import dev.emortal.velocity.lang.ChatMessages;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public final class FriendDenySubs {
    private static final Logger LOGGER = LoggerFactory.getLogger(FriendAddSub.class);

    private final RelationshipService relationshipService;

    public FriendDenySubs(@NotNull RelationshipService relationshipService) {
        this.relationshipService = relationshipService;
    }

    public void executeRevoke(@NotNull CommandContext<CommandSource> context) {
        this.executeCommon(context, ChatMessages.FRIEND_REQUEST_REVOKED, ChatMessages.ERROR_NO_FRIEND_REQUEST_SENT);
    }

    public void executeDeny(@NotNull CommandContext<CommandSource> context) {
        this.executeCommon(context, ChatMessages.FRIEND_REQUEST_DENIED, ChatMessages.ERROR_NO_FRIEND_REQUEST_RECEIVED);
    }

    private void executeCommon(@NotNull CommandContext<CommandSource> context, @NotNull ChatMessages denied, @NotNull ChatMessages noRequest) {
        Player player = (Player) context.getSource();
        String targetUsername = context.getArgument("username", String.class);

        PlayerResolver.CachedMcPlayer target;
        try {
            target = PlayerResolver.getPlayerData(targetUsername);
        } catch (StatusException exception) {
            LOGGER.error("Failed to get player data for '{}'", targetUsername, exception);
            ChatMessages.GENERIC_ERROR.send(player);
            return;
        }

        if (target == null) {
            ChatMessages.PLAYER_NOT_FOUND.send(player, Component.text(targetUsername));
            return;
        }

        String correctedUsername = target.username(); // this will have correct capitalisation
        UUID targetId = target.uuid();

        RelationshipProto.DenyFriendRequestResponse.DenyFriendRequestResult result;
        try {
            result = this.relationshipService.denyFriendRequest(player.getUniqueId(), targetId);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to deny friend request from '{}' to '{}'", correctedUsername, player.getUsername(), exception);
            ChatMessages.GENERIC_ERROR.send(player);
            return;
        }

        switch (result) {
            case DENIED -> denied.send(player, Component.text(correctedUsername));
            case NO_REQUEST -> noRequest.send(player, Component.text(correctedUsername));
        }
    }
}
