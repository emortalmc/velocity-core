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
import dev.emortal.velocity.relationships.FriendCache;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

final class FriendRemoveSub implements EmortalCommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(FriendRemoveSub.class);

    private final @NotNull RelationshipService relationshipService;
    private final @NotNull FriendCache friendCache;
    private final @NotNull PlayerResolver playerResolver;

    FriendRemoveSub(@NotNull RelationshipService relationshipService, @NotNull FriendCache friendCache, @NotNull PlayerResolver playerResolver) {
        this.relationshipService = relationshipService;
        this.friendCache = friendCache;
        this.playerResolver = playerResolver;
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
            ChatMessages.PLAYER_NOT_FOUND.send(player, targetUsername);
            return;
        }

        UUID targetId = target.uuid();
        String correctedUsername = target.username(); // this will have correct capitalisation

        RelationshipProto.RemoveFriendResponse.RemoveFriendResult result;
        try {
            result = this.relationshipService.removeFriend(player.getUniqueId(), player.getUsername(), targetId);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to remove friend '{}' from '{}'", correctedUsername, player.getUsername(), exception);
            ChatMessages.GENERIC_ERROR.send(player);
            return;
        }

        switch (result) {
            case REMOVED -> {
                this.friendCache.remove(player.getUniqueId(), targetId);
                ChatMessages.FRIEND_REMOVED.send(player, correctedUsername);
            }
            case NOT_FRIENDS -> ChatMessages.ERROR_NOT_FRIENDS.send(player, correctedUsername);
        }
    }
}
