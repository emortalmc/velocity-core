package cc.towerdefence.velocity.friends.commands;

import cc.towerdefence.api.service.FriendGrpc;
import cc.towerdefence.api.service.FriendProto;
import cc.towerdefence.api.service.McPlayerGrpc;
import cc.towerdefence.api.service.McPlayerProto;
import cc.towerdefence.api.utils.GrpcTimestampConverter;
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

public class FriendAddSub {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Logger LOGGER = LoggerFactory.getLogger(FriendAddSub.class);

    public static final String FRIEND_ADDED_MESSAGE = "<light_purple>You are now friends with <color:#c98fff><username></color>";
    private static final String ALREADY_FRIENDS_MESSAGE = "<light_purple>You are already friends with <color:#c98fff><username></color>";
    private static final String SENT_REQUEST_MESSAGE = "<light_purple>Sent a friend request to <color:#c98fff><username></color>";
    private static final String PRIVACY_BLOCKED_MESSAGE = "<color:#c98fff><username>'s</color> <light_purple>privacy settings don't allow you yo add them as a friend.";
    private static final String ALREADY_REQUESTED_MESSAGE = "<light_purple>You have already sent a friend request to <color:#c98fff><username></color>";

    private final McPlayerGrpc.McPlayerFutureStub mcPlayerService;
    private final FriendGrpc.FriendFutureStub friendService;
    private final FriendCache friendCache;

    public FriendAddSub(McPlayerGrpc.McPlayerFutureStub mcPlayerService, FriendGrpc.FriendFutureStub friendService, FriendCache friendCache) {
        this.mcPlayerService = mcPlayerService;
        this.friendService = friendService;
        this.friendCache = friendCache;
    }

    public int execute(CommandContext<CommandSource> context) {
        Player player = (Player) context.getSource();
        String targetUsername = context.getArgument("username", String.class);

        ListenableFuture<McPlayerProto.PlayerResponse> playerResponseFuture = this.mcPlayerService.getPlayerByUsername(
                McPlayerProto.PlayerUsernameRequest.newBuilder().setUsername(targetUsername).build()
        );
        Futures.addCallback(playerResponseFuture, FunctionalFutureCallback.create(
                playerResponse -> {
                    String correctedUsername = playerResponse.getCurrentUsername(); // this will have correct capitalisation
                    UUID targetId = UUID.fromString(playerResponse.getId());

                    ListenableFuture<FriendProto.AddFriendResponse> friendResponseFuture = this.friendService.addFriend(
                            FriendProto.AddFriendRequest.newBuilder()
                                    .setIssuerId(player.getUniqueId().toString())
                                    .setIssuerUsername(player.getUsername())
                                    .setTargetId(targetId.toString())
                                    .build()
                    );

                    Futures.addCallback(friendResponseFuture, FunctionalFutureCallback.create(
                            friendResponse -> {
                                player.sendMessage(switch (friendResponse.getResult()) {
                                    case FRIEND_ADDED -> {
                                        this.friendCache.add(player.getUniqueId(), new FriendCache.CachedFriend(targetId, GrpcTimestampConverter.reverse(friendResponse.getFriendsSince())));
                                        yield MINI_MESSAGE.deserialize(FRIEND_ADDED_MESSAGE, Placeholder.component("username", Component.text(correctedUsername)));
                                    }
                                    case ALREADY_FRIENDS ->
                                            MINI_MESSAGE.deserialize(ALREADY_FRIENDS_MESSAGE, Placeholder.component("username", Component.text(correctedUsername)));
                                    case REQUEST_SENT ->
                                            MINI_MESSAGE.deserialize(SENT_REQUEST_MESSAGE, Placeholder.component("username", Component.text(correctedUsername)));
                                    case PRIVACY_BLOCKED ->
                                            MINI_MESSAGE.deserialize(PRIVACY_BLOCKED_MESSAGE, Placeholder.component("username", Component.text(correctedUsername)));
                                    case ALREADY_REQUESTED ->
                                            MINI_MESSAGE.deserialize(ALREADY_REQUESTED_MESSAGE, Placeholder.component("username", Component.text(correctedUsername)));
                                    case UNRECOGNIZED -> {
                                        LOGGER.error("Unrecognised friend response: {}", friendResponse);
                                        yield Component.text("An error occurred");
                                    }
                                });
                            },
                            error -> {
                                LOGGER.error("Failed to send friend request", error);
                                player.sendMessage(Component.text("Failed to send friend request to " + correctedUsername));
                            }
                    ), ForkJoinPool.commonPool());
                },
                throwable -> {
                    LOGGER.error("Failed to get player by username: ", throwable);
                    player.sendMessage(Component.text("An error occurred while trying to add a friend."));
                }
        ), ForkJoinPool.commonPool());
        return 1;
    }
}
