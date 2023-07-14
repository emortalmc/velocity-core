package dev.emortal.velocity.relationships.commands.friend;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.grpc.relationship.RelationshipProto;
import dev.emortal.api.service.relationship.RelationshipService;
import dev.emortal.api.utils.resolvers.PlayerResolver;
import dev.emortal.velocity.lang.TempLang;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class FriendDenySubs {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Logger LOGGER = LoggerFactory.getLogger(FriendAddSub.class);

    private static final String FRIEND_REQUEST_DENIED_MESSAGE = "<light_purple>Removed your friend request from <color:#c98fff><username></color>";
    private static final String FRIEND_REQUEST_REVOKED_MESSAGE = "<light_purple>Revoked your friend request to <color:#c98fff><username></color>";
    private static final String NO_REQUEST_RECEIVED_MESSAGE = "<light_purple>You have not received a friend request from <color:#c98fff><username></color>";
    private static final String NO_REQUEST_SENT_MESSAGE = "<light_purple>You have not sent a friend request to <color:#c98fff><username></color>";

    private final RelationshipService relationshipService;

    public FriendDenySubs(@NotNull RelationshipService relationshipService) {
        this.relationshipService = relationshipService;
    }

    public void executeRevoke(@NotNull CommandContext<CommandSource> context) {
        this.executeCommon(context, FRIEND_REQUEST_REVOKED_MESSAGE, NO_REQUEST_SENT_MESSAGE);
    }

    public void executeDeny(@NotNull CommandContext<CommandSource> context) {
        this.executeCommon(context, FRIEND_REQUEST_DENIED_MESSAGE, NO_REQUEST_RECEIVED_MESSAGE);
    }

    private void executeCommon(@NotNull CommandContext<CommandSource> context, @NotNull String deniedMessage, @NotNull String noRequestMessage) {
        Player player = (Player) context.getSource();
        String targetUsername = context.getArgument("username", String.class);

        PlayerResolver.CachedMcPlayer target;
        try {
            target = PlayerResolver.getPlayerData(targetUsername);
        } catch (StatusException exception) {
            LOGGER.error("Failed to retrieve player UUID", exception);
            player.sendMessage(Component.text("An unknown error occurred", NamedTextColor.RED));
            return;
        }

        if (target == null) {
            TempLang.PLAYER_NOT_FOUND.send(player, Placeholder.unparsed("search_username", targetUsername));
            return;
        }

        String correctedUsername = target.username(); // this will have correct capitalisation
        UUID targetId = target.uuid();

        RelationshipProto.DenyFriendRequestResponse.DenyFriendRequestResult result;
        try {
            result = this.relationshipService.denyFriendRequest(player.getUniqueId(), targetId);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to deny friend request", exception);
            player.sendMessage(Component.text("Failed to deny friend request for " + correctedUsername));
            return;
        }

        var usernamePlaceholder = Placeholder.parsed("username", correctedUsername);
        var message = switch (result) {
            case DENIED -> MINI_MESSAGE.deserialize(deniedMessage, usernamePlaceholder);
            case NO_REQUEST -> MINI_MESSAGE.deserialize(noRequestMessage, usernamePlaceholder);
            case UNRECOGNIZED -> {
                LOGGER.error("An error occurred denying a friend request from {} to {}", player.getUsername(), correctedUsername);
                yield Component.text("An error occurred");
            }
        };
        player.sendMessage(message);
    }
}
