package dev.emortal.velocity.friends.commands;

import com.google.common.util.concurrent.Futures;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.grpc.mcplayer.McPlayerGrpc;
import dev.emortal.api.grpc.mcplayer.McPlayerProto;
import dev.emortal.api.grpc.relationship.RelationshipGrpc;
import dev.emortal.api.grpc.relationship.RelationshipProto;
import dev.emortal.api.utils.GrpcTimestampConverter;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import dev.emortal.velocity.utils.DurationFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;

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


    private final RelationshipGrpc.RelationshipFutureStub relationshipService;
    private final McPlayerGrpc.McPlayerFutureStub mcPlayerService;

    public FriendRequestsSub(RelationshipGrpc.RelationshipFutureStub relationshipService, McPlayerGrpc.McPlayerFutureStub mcPlayerService) {
        this.relationshipService = relationshipService;
        this.mcPlayerService = mcPlayerService;
    }

    public int executeIncoming(CommandContext<CommandSource> context) {
        return this.execute(context, true);
    }

    public int executeOutgoing(CommandContext<CommandSource> context) {
        return this.execute(context, false);
    }

    private int execute(CommandContext<CommandSource> context, boolean incoming) {
        Player player = (Player) context.getSource();
        int page = context.getArguments().containsKey("page") ? context.getArgument("page", Integer.class) : 1;

        var pendingResponseFuture = this.relationshipService.getPendingFriendRequestList(
                RelationshipProto.GetPendingFriendRequestListRequest.newBuilder()
                        .setIssuerId(player.getUniqueId().toString())
                        .setIncoming(incoming)
                        .build()
        );

        Futures.addCallback(pendingResponseFuture, FunctionalFutureCallback.create(
                pendingResponse -> {
                    var pendingFriends = pendingResponse.getRequestsList();
                    if (pendingFriends.isEmpty()) {
                        if (incoming) player.sendMessage(NO_INCOMING_REQUESTS_MESSAGE);
                        else player.sendMessage(NO_OUTGOING_REQUESTS_MESSAGE);
                        return;
                    }
                    List<String> friendIds = pendingFriends.stream().map(friendPlayer -> {
                        if (incoming) return friendPlayer.getRequesterId();
                        else return friendPlayer.getTargetId();
                    }).toList();

                    var playersResponseFuture = this.mcPlayerService.getPlayers(McPlayerProto.GetPlayersRequest.newBuilder()
                            .addAllPlayerIds(friendIds).build());

                    Futures.addCallback(playersResponseFuture, FunctionalFutureCallback.create(
                            playersResponse -> {
                                Map<UUID, String> usernameMap = new HashMap<>();
                                for (var playerResponse : playersResponse.getPlayersList()) {
                                    usernameMap.put(UUID.fromString(playerResponse.getId()), playerResponse.getCurrentUsername());
                                }

                                int totalPages = (int) Math.ceil(pendingFriends.size() / 10.0);
                                int limitedPage = Math.min(totalPages, page);

                                TextComponent.Builder message = Component.text()
                                        .append(MINI_MESSAGE.deserialize(incoming ? INCOMING_MESSAGE_TITLE : OUTGOING_MESSAGE_TITLE,
                                                Placeholder.parsed("page", String.valueOf(limitedPage)),
                                                Placeholder.parsed("max_page", String.valueOf(totalPages))))
                                        .append(Component.newline());

                                for (int i = (page - 1) * 10; i < page * 10 && i < pendingFriends.size(); i++) {
                                    var requestedFriend = pendingFriends.get(i);
                                    String username = usernameMap.get(UUID.fromString(incoming ? requestedFriend.getRequesterId() : requestedFriend.getTargetId()));

                                    message.append(MINI_MESSAGE.deserialize(incoming ? INCOMING_MESSAGE_LINE : OUTGOING_MESSAGE_LINE,
                                                    Placeholder.parsed("time", DurationFormatter.formatShortFromInstant(GrpcTimestampConverter.reverse(requestedFriend.getRequestTime()))),
                                                    Placeholder.parsed("username", username)))
                                            .append(Component.newline());
                                }

                                message.append(incoming ? INCOMING_MESSAGE_FOOTER : OUTGOING_MESSAGE_FOOTER);
                                player.sendMessage(message.build());
                            },
                            error -> {
                                LOGGER.error("Failed to get players: ", error);
                                player.sendMessage(Component.text("Failed to get players", NamedTextColor.RED));
                            }
                    ), ForkJoinPool.commonPool());
                },
                throwable -> {
                    LOGGER.error("Failed to get pending friend requests: ", throwable);
                    player.sendMessage(Component.text("Failed to get pending friend requests"));
                }
        ), ForkJoinPool.commonPool());

        return 1;
    }
}
