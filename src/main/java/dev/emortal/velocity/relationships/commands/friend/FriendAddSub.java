package dev.emortal.velocity.relationships.commands.friend;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.service.relationship.AddFriendResult;
import dev.emortal.api.service.relationship.RelationshipService;
import dev.emortal.velocity.command.ArgumentProvider;
import dev.emortal.velocity.command.EmortalCommandExecutor;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.player.resolver.CachedMcPlayer;
import dev.emortal.velocity.player.resolver.PlayerResolver;
import dev.emortal.velocity.relationships.FriendCache;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

final class FriendAddSub implements EmortalCommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(FriendAddSub.class);

    private final RelationshipService relationshipService;
    private final FriendCache friendCache;
    private final PlayerResolver playerResolver;

    FriendAddSub(@NotNull RelationshipService relationshipService, @NotNull FriendCache friendCache, @NotNull PlayerResolver playerResolver) {
        this.relationshipService = relationshipService;
        this.friendCache = friendCache;
        this.playerResolver = playerResolver;
    }

    @Override
    public void execute(@NotNull CommandSource source, @NotNull ArgumentProvider arguments) {
        Player player = (Player) source;
        String targetUsername = arguments.getArgument("username", String.class);

        if (player.getUsername().equalsIgnoreCase(targetUsername)) {
            ChatMessages.ERROR_CANNOT_FRIEND_SELF.send(player);
            return;
        }

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

        AddFriendResult result;
        try {
            result = this.relationshipService.addFriend(player.getUniqueId(), player.getUsername(), targetId);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to send friend request from '{}' to '{}'", player.getUsername(), correctedUsername, exception);
            ChatMessages.GENERIC_ERROR.send(player);
            return;
        }

        switch (result) {
            case AddFriendResult.RequestSent() -> ChatMessages.SENT_FRIEND_REQUEST.send(player, Component.text(correctedUsername));
            case AddFriendResult.FriendAdded(Instant friendsSince) -> {
                this.friendCache.add(player.getUniqueId(), new FriendCache.CachedFriend(targetId, friendsSince));
                ChatMessages.FRIEND_ADDED.send(player, Component.text(correctedUsername));
            }
            case AddFriendResult.Error error -> {
                switch (error) {
                    case ALREADY_FRIENDS -> ChatMessages.ERROR_ALREADY_FRIENDS.send(player, Component.text(correctedUsername));
                    case PRIVACY_BLOCKED -> ChatMessages.ERROR_PRIVACY_BLOCKED.send(player, Component.text(correctedUsername));
                    case ALREADY_REQUESTED -> ChatMessages.ERROR_FRIEND_ALREADY_REQUESTED.send(player, Component.text(correctedUsername));
                    case YOU_BLOCKED -> ChatMessages.ERROR_CANNOT_FRIEND_BLOCKED.send(player, Component.text(correctedUsername));
                }
            }
        }
    }
}
