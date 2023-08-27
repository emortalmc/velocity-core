package dev.emortal.velocity.relationships.commands.friend;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.model.mcplayer.McPlayer;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.api.service.relationship.RelationshipService;
import dev.emortal.api.service.relationship.RequestedFriend;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.utils.DurationFormatter;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FriendRequestsSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(FriendRequestsSub.class);

    private static final Component INCOMING_MESSAGE_FOOTER = Component.text("-----------------------------------", NamedTextColor.LIGHT_PURPLE);
    private static final Component OUTGOING_MESSAGE_FOOTER = Component.text("--------------------------------", NamedTextColor.LIGHT_PURPLE);

    private final RelationshipService relationshipService;
    private final McPlayerService mcPlayerService;

    public FriendRequestsSub(@NotNull RelationshipService relationshipService, @NotNull McPlayerService mcPlayerService) {
        this.relationshipService = relationshipService;
        this.mcPlayerService = mcPlayerService;
    }

    public void executeIncoming(@NotNull CommandContext<CommandSource> context) {
        this.execute(context, true);
    }

    public void executeOutgoing(@NotNull CommandContext<CommandSource> context) {
        this.execute(context, false);
    }

    private void execute(@NotNull CommandContext<CommandSource> context, boolean incoming) {
        Player player = (Player) context.getSource();
        int page = context.getArguments().containsKey("page") ? context.getArgument("page", Integer.class) : 1;

        List<RequestedFriend> friendRequests;
        try {
            friendRequests = incoming ? this.relationshipService.listPendingIncomingFriendRequests(player.getUniqueId()) :
                    this.relationshipService.listPendingOutgoingFriendRequests(player.getUniqueId());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to get pending friend requests: ", exception);
            player.sendMessage(Component.text("Failed to get pending friend requests"));
            return;
        }

        if (friendRequests.isEmpty()) {
            if (incoming) {
                ChatMessages.ERROR_NO_INCOMING_FRIEND_REQUESTS.send(player);
            } else {
                ChatMessages.ERROR_NO_OUTGOING_FRIEND_REQUESTS.send(player);
            }
            return;
        }

        List<UUID> friendIds = new ArrayList<>();
        for (RequestedFriend friendRequest : friendRequests) {
            friendIds.add(incoming ? friendRequest.requesterId() : friendRequest.targetId());
        }

        List<McPlayer> players;
        try {
            players = this.mcPlayerService.getPlayersById(friendIds);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to resolve friends from IDs '{}'", friendIds, exception);
            ChatMessages.GENERIC_ERROR.send(player);
            return;
        }

        Map<UUID, String> usernameMap = new HashMap<>();
        for (var playerResponse : players) {
            usernameMap.put(UUID.fromString(playerResponse.getId()), playerResponse.getCurrentUsername());
        }

        int totalPages = (int) Math.ceil(friendRequests.size() / 10.0);
        int limitedPage = Math.min(totalPages, page);

        ChatMessages header = incoming ? ChatMessages.INCOMING_FRIEND_REQUESTS_HEADER : ChatMessages.OUTGOING_FRIEND_REQUESTS_HEADER;
        TextComponent.Builder message = Component.text()
                .append(header.parse(Component.text(limitedPage), Component.text(totalPages)))
                .appendNewline();

        for (int i = (page - 1) * 10; i < page * 10 && i < friendRequests.size(); i++) {
            RequestedFriend requestedFriend = friendRequests.get(i);
            String username = usernameMap.get(incoming ? requestedFriend.requesterId() : requestedFriend.targetId());

            ChatMessages line = incoming ? ChatMessages.INCOMING_FRIEND_REQUEST_LINE : ChatMessages.OUTGOING_FRIEND_REQUEST_LINE;
            String duration = DurationFormatter.formatShortFromInstant(requestedFriend.requestTime());
            message.append(line.parse(Component.text(duration), Component.text(username))).appendNewline();
        }

        message.append(incoming ? INCOMING_MESSAGE_FOOTER : OUTGOING_MESSAGE_FOOTER);
        player.sendMessage(message.build());
    }
}
