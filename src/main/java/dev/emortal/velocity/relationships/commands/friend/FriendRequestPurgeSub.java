package dev.emortal.velocity.relationships.commands.friend;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.service.relationship.RelationshipService;
import dev.emortal.velocity.lang.ChatMessages;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FriendRequestPurgeSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(FriendRequestPurgeSub.class);

    private final RelationshipService friendService;

    public FriendRequestPurgeSub(@NotNull RelationshipService friendService) {
        this.friendService = friendService;
    }

    public void executeIncoming(@NotNull CommandContext<CommandSource> context) {
        Player player = (Player) context.getSource();

        int deniedRequests;
        try {
            deniedRequests = this.friendService.denyAllIncomingFriendRequests(player.getUniqueId());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to purge incoming friend requests for '{}'", player.getUsername(), exception);
            ChatMessages.GENERIC_ERROR.send(player);
            return;
        }

        ChatMessages.PURGED_INCOMING_FRIEND_REQUESTS.send(player, Component.text(deniedRequests));
    }

    public void executeOutgoing(@NotNull CommandContext<CommandSource> context) {
        Player player = (Player) context.getSource();

        int deniedRequests;
        try {
            deniedRequests = this.friendService.denyAllOutgoingFriendRequests(player.getUniqueId());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to purge outgoing friend requests for '{}'", player.getUsername(), exception);
            ChatMessages.GENERIC_ERROR.send(player);
            return;
        }

        ChatMessages.PURGED_OUTGOING_FRIEND_REQUESTS.send(player, Component.text(deniedRequests));
    }
}
