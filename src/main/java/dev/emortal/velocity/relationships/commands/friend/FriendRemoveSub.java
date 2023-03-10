package dev.emortal.velocity.relationships.commands.friend;


import com.google.common.util.concurrent.Futures;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.grpc.mcplayer.McPlayerGrpc;
import dev.emortal.api.grpc.mcplayer.McPlayerProto;
import dev.emortal.api.grpc.relationship.RelationshipGrpc;
import dev.emortal.api.grpc.relationship.RelationshipProto;
import dev.emortal.api.model.mcplayer.McPlayer;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import dev.emortal.velocity.relationships.FriendCache;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ForkJoinPool;

public class FriendRemoveSub {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Logger LOGGER = LoggerFactory.getLogger(FriendRemoveSub.class);

    private static final String FRIEND_REMOVED_MESSAGE = "<light_purple>You are no longer friends with <color:#c98fff><username></color>";
    private static final String NOT_FRIENDS_MESSAGE = "<light_purple>You are not friends with <color:#c98fff><username></color>";

    private final McPlayerGrpc.McPlayerFutureStub mcPlayerService;
    private final RelationshipGrpc.RelationshipFutureStub relationshipService;
    private final FriendCache friendCache;

    public FriendRemoveSub(McPlayerGrpc.McPlayerFutureStub mcPlayerService, RelationshipGrpc.RelationshipFutureStub relationshipService, FriendCache friendCache) {
        this.mcPlayerService = mcPlayerService;
        this.relationshipService = relationshipService;
        this.friendCache = friendCache;
    }

    public int execute(CommandContext<CommandSource> context) {
        Player player = (Player) context.getSource();
        String targetUsername = context.getArgument("username", String.class);

        var playerResponseFuture = this.mcPlayerService.getPlayerByUsername(
                McPlayerProto.PlayerUsernameRequest.newBuilder().setUsername(targetUsername).build()
        );

        Futures.addCallback(playerResponseFuture, FunctionalFutureCallback.create(
                playerResponse -> {
                    McPlayer mcPlayer = playerResponse.getPlayer();
                    String correctedUsername = mcPlayer.getCurrentUsername(); // this will have correct capitalisation
                    UUID targetId = UUID.fromString(mcPlayer.getId());

                    var removeFriendResponseFuture = this.relationshipService.removeFriend(RelationshipProto.RemoveFriendRequest.newBuilder()
                            .setSenderId(player.getUniqueId().toString())
                            .setSenderUsername(player.getUsername())
                            .setTargetId(targetId.toString())
                            .build());

                    Futures.addCallback(removeFriendResponseFuture, FunctionalFutureCallback.create(
                            removeFriendResponse -> {
                                player.sendMessage(switch (removeFriendResponse.getResult()) {
                                    case REMOVED -> {
                                        this.friendCache.remove(player.getUniqueId(), targetId);
                                        yield MINI_MESSAGE.deserialize(FRIEND_REMOVED_MESSAGE, Placeholder.component("username", Component.text(correctedUsername)));
                                    }
                                    case NOT_FRIENDS ->
                                            MINI_MESSAGE.deserialize(NOT_FRIENDS_MESSAGE, Placeholder.component("username", Component.text(correctedUsername)));
                                    case UNRECOGNIZED -> {
                                        LOGGER.error("Unrecognised friend response: {}", removeFriendResponse);
                                        yield Component.text("An error occurred");
                                    }
                                });
                            },
                            throwable -> {
                                LOGGER.error("Failed to remove friend: ", throwable);
                                player.sendMessage(Component.text("Failed to remove friend " + correctedUsername));
                            }
                    ), ForkJoinPool.commonPool());
                },
                throwable -> {
                    LOGGER.error("Failed to get player by username", throwable);
                    player.sendMessage(Component.text("Failed to remove friend " + targetUsername));
                }
        ), ForkJoinPool.commonPool());

        return 1;
    }
}
