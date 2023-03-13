package dev.emortal.velocity.relationships.commands.friend;

import com.google.common.util.concurrent.Futures;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.grpc.relationship.RelationshipGrpc;
import dev.emortal.api.grpc.relationship.RelationshipProto;
import dev.emortal.api.model.relationship.FriendRequest;
import dev.emortal.api.utils.GrpcTimestampConverter;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import dev.emortal.api.utils.resolvers.PlayerResolver;
import dev.emortal.velocity.lang.TempLang;
import dev.emortal.velocity.relationships.FriendCache;
import io.grpc.Status;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    private static final String PRIVACY_BLOCKED_MESSAGE = "<color:#c98fff><username>'s</color> <light_purple>privacy settings don't allow you you add them as a friend.";
    private static final String ALREADY_REQUESTED_MESSAGE = "<light_purple>You have already sent a friend request to <color:#c98fff><username></color>";
    private static final String YOU_BLOCKED_MESSAGE = "<red>You cannot add <color:#c98fff><username></color> as a friend as you have blocked them!</red>";

    private final RelationshipGrpc.RelationshipFutureStub relationshipService;
    private final FriendCache friendCache;

    public FriendAddSub(RelationshipGrpc.RelationshipFutureStub relationshipService, FriendCache friendCache) {
        this.relationshipService = relationshipService;
        this.friendCache = friendCache;
    }

    public int execute(CommandContext<CommandSource> context) {
        Player player = (Player) context.getSource();
        String targetUsername = context.getArgument("username", String.class);

        if (player.getUsername().equalsIgnoreCase(targetUsername)) {
            player.sendMessage(Component.text("You can't add yourself as a friend.", NamedTextColor.RED));
            return 1;
        }

        PlayerResolver.retrievePlayerData(targetUsername, cachedMcPlayer -> {
            String correctedUsername = cachedMcPlayer.username();
            UUID targetId = cachedMcPlayer.uuid();

            var friendResponseFuture = this.relationshipService.addFriend(
                    RelationshipProto.AddFriendRequest.newBuilder()
                            .setRequest(FriendRequest.newBuilder()
                                    .setSenderId(player.getUniqueId().toString())
                                    .setSenderUsername(player.getUsername())
                                    .setTargetId(targetId.toString()))
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
                            case YOU_BLOCKED ->
                                    MINI_MESSAGE.deserialize(YOU_BLOCKED_MESSAGE, Placeholder.component("username", Component.text(correctedUsername)));
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
        }, errorStatus -> {
            if (errorStatus.getCode() == Status.Code.NOT_FOUND) {
                TempLang.PLAYER_NOT_FOUND.send(player, Placeholder.unparsed("search_username", targetUsername));
            }

            LOGGER.error("Failed to retrieve player UUID", errorStatus.asRuntimeException());
            player.sendMessage(Component.text("An unknown error occurred", NamedTextColor.RED));

        });
        return 1;
    }
}
