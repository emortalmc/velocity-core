package dev.emortal.velocity.friends.commands;

import dev.emortal.api.service.FriendGrpc;
import dev.emortal.api.service.FriendProto;
import dev.emortal.api.service.McPlayerGrpc;
import dev.emortal.api.utils.resolvers.PlayerResolver;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import io.grpc.Status;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ForkJoinPool;

public class FriendDenySubs {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Logger LOGGER = LoggerFactory.getLogger(FriendAddSub.class);

    private static final String FRIEND_REQUEST_DENIED_MESSAGE = "<light_purple>Removed your friend request from <color:#c98fff><username></color>";
    private static final String FRIEND_REQUEST_REVOKED_MESSAGE = "<light_purple>Revoked your friend request to <color:#c98fff><username></color>";
    private static final String NO_REQUEST_RECEIVED_MESSAGE = "<light_purple>You have not received a friend request from <color:#c98fff><username></color>";
    private static final String NO_REQUEST_SENT_MESSAGE = "<light_purple>You have not sent a friend request to <color:#c98fff><username></color>";

    private final McPlayerGrpc.McPlayerFutureStub mcPlayerService;
    private final FriendGrpc.FriendFutureStub friendService;

    public FriendDenySubs(McPlayerGrpc.McPlayerFutureStub mcPlayerService, FriendGrpc.FriendFutureStub friendService) {
        this.mcPlayerService = mcPlayerService;
        this.friendService = friendService;
    }

    public int executeRevoke(CommandContext<CommandSource> context) {
        return this.executeCommon(context, FRIEND_REQUEST_REVOKED_MESSAGE, NO_REQUEST_SENT_MESSAGE);
    }

    public int executeDeny(CommandContext<CommandSource> context) {
        return this.executeCommon(context, FRIEND_REQUEST_DENIED_MESSAGE, NO_REQUEST_RECEIVED_MESSAGE);
    }

    private int executeCommon(CommandContext<CommandSource> context, String deniedMessage, String noRequestMessage) {
        Player player = (Player) context.getSource();
        String targetUsername = context.getArgument("username", String.class);

        PlayerResolver.retrievePlayerData(targetUsername, cachedMcPlayer -> {
            String correctedUsername = cachedMcPlayer.username(); // this will have correct capitalisation
            UUID targetId = cachedMcPlayer.uuid();

            ListenableFuture<FriendProto.DenyFriendRequestResponse> denyResponseFuture = this.friendService.denyFriendRequest(
                    FriendProto.DenyFriendRequestRequest.newBuilder()
                            .setIssuerId(player.getUniqueId().toString())
                            .setTargetId(targetId.toString())
                            .build()
            );

            Futures.addCallback(denyResponseFuture, FunctionalFutureCallback.create(
                    denyResponse -> {
                        player.sendMessage(switch (denyResponse.getResult()) {
                            case DENIED ->
                                    MINI_MESSAGE.deserialize(deniedMessage, Placeholder.parsed("username", correctedUsername));
                            case NO_REQUEST ->
                                    MINI_MESSAGE.deserialize(noRequestMessage, Placeholder.parsed("username", correctedUsername));
                            case UNRECOGNIZED -> {
                                LOGGER.error("Unrecognised request deny response: {}", denyResponse);
                                yield Component.text("An error occurred");
                            }
                        });
                    },
                    error -> {
                        LOGGER.error("Failed to deny friend request", error);
                        player.sendMessage(Component.text("Failed to deny friend request for " + correctedUsername));
                    }), ForkJoinPool.commonPool());
        }, errorStatus -> {
            if (errorStatus.getCode() == Status.Code.NOT_FOUND) {
                player.sendMessage(Component.text("Could not find player " + targetUsername, NamedTextColor.RED));
            } else {
                LOGGER.error("Failed to retrieve player UUID", errorStatus.asRuntimeException());
                player.sendMessage(Component.text("An unknown error occurred", NamedTextColor.RED));
            }
        });
        return 1;
    }
}
