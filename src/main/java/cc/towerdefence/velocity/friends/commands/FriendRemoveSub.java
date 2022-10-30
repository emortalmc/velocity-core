package cc.towerdefence.velocity.friends.commands;

import cc.towerdefence.api.service.FriendGrpc;
import cc.towerdefence.api.service.FriendProto;
import cc.towerdefence.api.service.McPlayerGrpc;
import cc.towerdefence.api.service.McPlayerProto;
import cc.towerdefence.velocity.friends.FriendCache;
import cc.towerdefence.velocity.utils.FunctionalFutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
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
    private final FriendGrpc.FriendFutureStub friendService;
    private final FriendCache friendCache;

    public FriendRemoveSub(McPlayerGrpc.McPlayerFutureStub mcPlayerService, FriendGrpc.FriendFutureStub friendService, FriendCache friendCache) {
        this.mcPlayerService = mcPlayerService;
        this.friendService = friendService;
        this.friendCache = friendCache;
    }

    public int execute(CommandContext<CommandSource> context) {
        Player player = (Player) context.getSource();
        String targetUsername = context.getArgument("username", String.class);

        ListenableFuture<McPlayerProto.PlayerResponse> playerResponseFuture = this.mcPlayerService
                .getPlayerByUsername(McPlayerProto.PlayerUsernameRequest.newBuilder().setUsername(targetUsername).build());

        Futures.addCallback(playerResponseFuture, FunctionalFutureCallback.create(
                playerResponse -> {
                    String correctedUsername = playerResponse.getCurrentUsername(); // this will have correct capitalisation
                    UUID targetId = UUID.fromString(playerResponse.getId());

                    ListenableFuture<FriendProto.RemoveFriendResponse> removeFriendResponseFuture = this.friendService.removeFriend(FriendProto.RemoveFriendRequest.newBuilder()
                            .setIssuerId(player.getUniqueId().toString())
                            .setIssuerUsername(player.getUsername())
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
