package dev.emortal.velocity.relationships.commands.friend;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.model.mcplayer.McPlayer;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.api.service.relationship.RelationshipService;
import dev.emortal.api.service.relationship.RequestedFriend;
import dev.emortal.velocity.utils.DurationFormatter;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FriendRequestsSub {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Logger LOGGER = LoggerFactory.getLogger(FriendRequestsSub.class);

    private static final Component NO_INCOMING_REQUESTS_MESSAGE = Component.text("You have no incoming friend requests", NamedTextColor.LIGHT_PURPLE);
    private static final String INCOMING_MESSAGE_TITLE = "<light_purple>--- Incoming Requests (Page <page>/<max_page>) ---</light_purple>";
    private static final String INCOMING_MESSAGE_LINE = "<light_purple><time> ago <dark_purple>- <light_purple><username> <dark_purple>| <green><click:run_command:'/friend add <username>'>Accept</click><dark_purple>/<red><click:run_command:'/friend deny <username>'>Deny</click>";
    private static final Component INCOMING_MESSAGE_FOOTER = Component.text("-----------------------------------", NamedTextColor.LIGHT_PURPLE);

    private static final Component NO_OUTGOING_REQUESTS_MESSAGE = Component.text("You have no outgoing friend requests", NamedTextColor.LIGHT_PURPLE);
    private static final String OUTGOING_MESSAGE_TITLE = "<light_purple>--- Outgoing Requests (Page <page>/<max_page>) ---</light_purple>";
    private static final String OUTGOING_MESSAGE_LINE = "<light_purple><time> ago <dark_purple>- <light_purple><username> <dark_purple>| <red><click:run_command:'/friend revoke <username>'>Revoke</click>";
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
                player.sendMessage(NO_INCOMING_REQUESTS_MESSAGE);
            } else {
                player.sendMessage(NO_OUTGOING_REQUESTS_MESSAGE);
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
            LOGGER.error("Failed to get players: ", exception);
            player.sendMessage(Component.text("Failed to get players", NamedTextColor.RED));
            return;
        }

        Map<UUID, String> usernameMap = new HashMap<>();
        for (var playerResponse : players) {
            usernameMap.put(UUID.fromString(playerResponse.getId()), playerResponse.getCurrentUsername());
        }

        int totalPages = (int) Math.ceil(friendRequests.size() / 10.0);
        int limitedPage = Math.min(totalPages, page);

        TextComponent.Builder message = Component.text()
                .append(MINI_MESSAGE.deserialize(incoming ? INCOMING_MESSAGE_TITLE : OUTGOING_MESSAGE_TITLE,
                        Placeholder.parsed("page", String.valueOf(limitedPage)),
                        Placeholder.parsed("max_page", String.valueOf(totalPages))))
                .append(Component.newline());

        for (int i = (page - 1) * 10; i < page * 10 && i < friendRequests.size(); i++) {
            RequestedFriend requestedFriend = friendRequests.get(i);
            String username = usernameMap.get(incoming ? requestedFriend.requesterId() : requestedFriend.targetId());

            message.append(MINI_MESSAGE.deserialize(incoming ? INCOMING_MESSAGE_LINE : OUTGOING_MESSAGE_LINE,
                            Placeholder.parsed("time", DurationFormatter.formatShortFromInstant(requestedFriend.requestTime())),
                            Placeholder.parsed("username", username)))
                    .append(Component.newline());
        }

        message.append(incoming ? INCOMING_MESSAGE_FOOTER : OUTGOING_MESSAGE_FOOTER);
        player.sendMessage(message.build());
    }
}
