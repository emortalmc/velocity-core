package dev.emortal.velocity.relationships.commands.friend;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.grpc.relationship.RelationshipProto;
import dev.emortal.api.service.relationship.RelationshipService;
import dev.emortal.velocity.command.ArgumentProvider;
import dev.emortal.velocity.command.EmortalCommandExecutor;
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

final class FriendDenySub implements EmortalCommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(FriendDenySub.class);

    static @NotNull FriendDenySub deny(@NotNull RelationshipService relationshipService, @NotNull PlayerResolver playerResolver) {
        return new FriendDenySub(relationshipService, playerResolver, ChatMessages.FRIEND_REQUEST_DENIED, ChatMessages.ERROR_NO_FRIEND_REQUEST_RECEIVED);
    }

    static @NotNull FriendDenySub revoke(@NotNull RelationshipService relationshipService, @NotNull PlayerResolver playerResolver) {
        return new FriendDenySub(relationshipService, playerResolver, ChatMessages.FRIEND_REQUEST_REVOKED, ChatMessages.ERROR_NO_FRIEND_REQUEST_SENT);
    }

    private final @NotNull RelationshipService relationshipService;
    private final @NotNull PlayerResolver playerResolver;
    private final @NotNull ChatMessages denied;
    private final @NotNull ChatMessages noRequest;

    private FriendDenySub(@NotNull RelationshipService relationshipService, @NotNull PlayerResolver playerResolver, @NotNull ChatMessages denied,
                          @NotNull ChatMessages noRequest) {
        this.relationshipService = relationshipService;
        this.playerResolver = playerResolver;
        this.denied = denied;
        this.noRequest = noRequest;
    }

    @Override
    public void execute(@NotNull CommandSource source, @NotNull ArgumentProvider arguments) {
        Player player = (Player) source;
        String targetUsername = arguments.getArgument("username", String.class);

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
