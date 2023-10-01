package dev.emortal.velocity.relationships.commands.friend;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.service.relationship.RelationshipService;
import dev.emortal.velocity.command.ArgumentProvider;
import dev.emortal.velocity.command.EmortalCommandExecutor;
import dev.emortal.velocity.lang.ChatMessages;
import io.grpc.StatusRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class FriendRequestPurgeSub implements EmortalCommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(FriendRequestPurgeSub.class);

    static @NotNull FriendRequestPurgeSub incoming(@NotNull RelationshipService relationshipService) {
        return new FriendRequestPurgeSub(relationshipService, true, ChatMessages.PURGED_INCOMING_FRIEND_REQUESTS);
    }

    static @NotNull FriendRequestPurgeSub outgoing(@NotNull RelationshipService relationshipService) {
        return new FriendRequestPurgeSub(relationshipService, false, ChatMessages.PURGED_OUTGOING_FRIEND_REQUESTS);
    }

    private final @NotNull RelationshipService friendService;
    private final boolean incoming;
    private final @NotNull ChatMessages.Args1<Integer> success;

    private FriendRequestPurgeSub(@NotNull RelationshipService friendService, boolean incoming, @NotNull ChatMessages.Args1<Integer> success) {
        this.friendService = friendService;
        this.incoming = incoming;
        this.success = success;
    }

    @Override
    public void execute(@NotNull CommandSource source, @NotNull ArgumentProvider arguments) {
        Player player = (Player) source;

        int deniedRequests;
        try {
            deniedRequests = this.friendService.denyAllFriendRequests(player.getUniqueId(), this.incoming);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to purge incoming friend requests for '{}'", player.getUsername(), exception);
            ChatMessages.GENERIC_ERROR.send(player);
            return;
        }

        this.success.send(player, deniedRequests);
    }
}
