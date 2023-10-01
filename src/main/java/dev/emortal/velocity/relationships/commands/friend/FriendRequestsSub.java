package dev.emortal.velocity.relationships.commands.friend;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.model.mcplayer.McPlayer;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.api.service.relationship.RelationshipService;
import dev.emortal.api.service.relationship.RequestedFriend;
import dev.emortal.velocity.command.ArgumentProvider;
import dev.emortal.velocity.command.EmortalCommandExecutor;
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

final class FriendRequestsSub implements EmortalCommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(FriendRequestsSub.class);

    private static final Component INCOMING_MESSAGE_FOOTER = Component.text("-----------------------------------", NamedTextColor.LIGHT_PURPLE);
    private static final Component OUTGOING_MESSAGE_FOOTER = Component.text("--------------------------------", NamedTextColor.LIGHT_PURPLE);

    private static final Context INCOMING = new Context(
            ChatMessages.ERROR_NO_INCOMING_FRIEND_REQUESTS,
            ChatMessages.INCOMING_FRIEND_REQUESTS_HEADER,
            ChatMessages.INCOMING_FRIEND_REQUEST_LINE,
            INCOMING_MESSAGE_FOOTER
    );
    private static final Context OUTGOING = new Context(
            ChatMessages.ERROR_NO_OUTGOING_FRIEND_REQUESTS,
            ChatMessages.OUTGOING_FRIEND_REQUESTS_HEADER,
            ChatMessages.OUTGOING_FRIEND_REQUEST_LINE,
            OUTGOING_MESSAGE_FOOTER
    );

    static @NotNull FriendRequestsSub incoming(@NotNull RelationshipService relationshipService, @NotNull McPlayerService playerService) {
        return new FriendRequestsSub(relationshipService, playerService, true, INCOMING);
    }

    static @NotNull FriendRequestsSub outgoing(@NotNull RelationshipService relationshipService, @NotNull McPlayerService playerService) {
        return new FriendRequestsSub(relationshipService, playerService, false, OUTGOING);
    }

    private final @NotNull RelationshipService relationshipService;
    private final @NotNull McPlayerService playerService;
    private final boolean incoming;
    private final @NotNull Context context;

    private FriendRequestsSub(@NotNull RelationshipService relationshipService, @NotNull McPlayerService playerService, boolean incoming,
                              @NotNull Context context) {
        this.relationshipService = relationshipService;
        this.playerService = playerService;
        this.incoming = incoming;
        this.context = context;
    }

    @Override
    public void execute(@NotNull CommandSource source, @NotNull ArgumentProvider arguments) {
        Player player = (Player) source;
        int page = arguments.hasArgument("page") ? arguments.getArgument("page", Integer.class) : 1;

        List<RequestedFriend> friendRequests;
        try {
            friendRequests = this.relationshipService.listPendingFriendRequests(player.getUniqueId(), this.incoming);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to get pending friend requests for '{}'", player.getUsername(), exception);
            ChatMessages.GENERIC_ERROR.send(player);
            return;
        }

        if (friendRequests.isEmpty()) {
            this.context.noMessages().send(player);
            return;
        }

        List<UUID> friendIds = new ArrayList<>();
        for (RequestedFriend friendRequest : friendRequests) {
            friendIds.add(this.incoming ? friendRequest.requesterId() : friendRequest.targetId());
        }

        List<McPlayer> players;
        try {
            players = this.playerService.getPlayersById(friendIds);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to resolve friends from IDs '{}'", friendIds, exception);
            ChatMessages.GENERIC_ERROR.send(player);
            return;
        }

        Map<UUID, String> usernameMap = new HashMap<>();
        for (McPlayer mcPlayer : players) {
            usernameMap.put(UUID.fromString(mcPlayer.getId()), mcPlayer.getCurrentUsername());
        }

        int totalPages = (int) Math.ceil(friendRequests.size() / 10.0);
        int limitedPage = Math.min(totalPages, page);

        TextComponent.Builder message = Component.text()
                .append(this.context.title().get(limitedPage, totalPages))
                .appendNewline();

        for (int i = (page - 1) * 10; i < page * 10 && i < friendRequests.size(); i++) {
            RequestedFriend requestedFriend = friendRequests.get(i);
            String username = usernameMap.get(this.incoming ? requestedFriend.requesterId() : requestedFriend.targetId());

            String duration = DurationFormatter.formatShortFromInstant(requestedFriend.requestTime());
            message.append(this.context.line().get(duration, username)).appendNewline();
        }

        message.append(this.context.footer());
        player.sendMessage(message.build());
    }

    private record Context(@NotNull ChatMessages.Args0 noMessages, @NotNull ChatMessages.Args2<Integer, Integer> title,
                           @NotNull ChatMessages.Args2<String, String> line, @NotNull Component footer) {
    }
}
