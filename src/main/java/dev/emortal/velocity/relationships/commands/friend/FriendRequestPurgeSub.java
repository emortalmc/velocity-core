package dev.emortal.velocity.relationships.commands.friend;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.service.relationship.RelationshipService;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FriendRequestPurgeSub {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Logger LOGGER = LoggerFactory.getLogger(FriendRequestPurgeSub.class);

    private static final String PURGED_INCOMING_MESSAGE = "<light_purple>Purged <count> incoming friend requests";
    private static final String PURGED_OUTGOING_MESSAGE = "<light_purple>Purged <count> outgoing friend requests";

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
            LOGGER.error("Failed to purge incoming friend requests", exception);
            player.sendMessage(Component.text("Failed to purge incoming friend requests", NamedTextColor.RED));
            return;
        }

        player.sendMessage(MINI_MESSAGE.deserialize(PURGED_INCOMING_MESSAGE, Placeholder.parsed("count", String.valueOf(deniedRequests))));
    }

    public void executeOutgoing(@NotNull CommandContext<CommandSource> context) {
        Player player = (Player) context.getSource();

        int deniedRequests;
        try {
            deniedRequests = this.friendService.denyAllOutgoingFriendRequests(player.getUniqueId());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to purge outgoing friend requests", exception);
            player.sendMessage(Component.text("Failed to purge outgoing friend requests", NamedTextColor.RED));
            return;
        }

        player.sendMessage(MINI_MESSAGE.deserialize(PURGED_OUTGOING_MESSAGE, Placeholder.parsed("count", String.valueOf(deniedRequests))));
    }
}
