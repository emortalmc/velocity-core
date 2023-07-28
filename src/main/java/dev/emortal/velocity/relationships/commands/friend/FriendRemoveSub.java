package dev.emortal.velocity.relationships.commands.friend;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.grpc.relationship.RelationshipProto;
import dev.emortal.api.model.mcplayer.McPlayer;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.api.service.relationship.RelationshipService;
import dev.emortal.velocity.relationships.FriendCache;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public final class FriendRemoveSub {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Logger LOGGER = LoggerFactory.getLogger(FriendRemoveSub.class);

    private static final String FRIEND_REMOVED_MESSAGE = "<light_purple>You are no longer friends with <color:#c98fff><username></color>";
    private static final String NOT_FRIENDS_MESSAGE = "<light_purple>You are not friends with <color:#c98fff><username></color>";

    private final McPlayerService mcPlayerService;
    private final RelationshipService relationshipService;
    private final FriendCache friendCache;

    public FriendRemoveSub(@NotNull McPlayerService mcPlayerService, @NotNull RelationshipService relationshipService, @NotNull FriendCache friendCache) {
        this.mcPlayerService = mcPlayerService;
        this.relationshipService = relationshipService;
        this.friendCache = friendCache;
    }

    public void execute(@NotNull CommandContext<CommandSource> context) {
        Player player = (Player) context.getSource();
        String targetUsername = context.getArgument("username", String.class);

        McPlayer target;
        try {
            target = this.mcPlayerService.getPlayerByUsername(targetUsername);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to get player by username", exception);
            player.sendMessage(Component.text("Failed to remove friend " + targetUsername));
            return;
        }

        if (target == null) {
            LOGGER.error("Failed to get player {} by username", targetUsername);
            player.sendMessage(Component.text("Failed to remove friend " + targetUsername));
            return;
        }

        UUID targetId = UUID.fromString(target.getId());
        String correctedUsername = target.getCurrentUsername(); // this will have correct capitalisation

        RelationshipProto.RemoveFriendResponse.RemoveFriendResult result;
        try {
            result = this.relationshipService.removeFriend(player.getUniqueId(), player.getUsername(), targetId);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to remove friend: ", exception);
            player.sendMessage(Component.text("Failed to remove friend " + correctedUsername));
            return;
        }

        var usernamePlaceholder = Placeholder.component("username", Component.text(correctedUsername));
        var message = switch (result) {
            case REMOVED -> {
                this.friendCache.remove(player.getUniqueId(), targetId);
                yield MINI_MESSAGE.deserialize(FRIEND_REMOVED_MESSAGE, usernamePlaceholder);
            }
            case NOT_FRIENDS -> MINI_MESSAGE.deserialize(NOT_FRIENDS_MESSAGE, usernamePlaceholder);
            case UNRECOGNIZED -> {
                LOGGER.error("An error occurred while {} tried to remove {} as a friend", player.getUsername(), correctedUsername);
                yield Component.text("An error occurred");
            }
        };
        player.sendMessage(message);
    }
}
