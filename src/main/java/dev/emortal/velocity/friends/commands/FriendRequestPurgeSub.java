package dev.emortal.velocity.friends.commands;

import com.google.common.util.concurrent.Futures;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.grpc.relationship.RelationshipGrpc;
import dev.emortal.api.grpc.relationship.RelationshipProto;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;

public class FriendRequestPurgeSub {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Logger LOGGER = LoggerFactory.getLogger(FriendRequestPurgeSub.class);

    private static final String PURGED_INCOMING_MESSAGE = "<light_purple>Purged <count> incoming friend requests";
    private static final String PURGED_OUTGOING_MESSAGE = "<light_purple>Purged <count> outgoing friend requests";

    private final RelationshipGrpc.RelationshipFutureStub friendService;

    public FriendRequestPurgeSub(RelationshipGrpc.RelationshipFutureStub friendService) {
        this.friendService = friendService;
    }

    public int executeIncoming(CommandContext<CommandSource> context) {
        return this.execute(context, true);
    }

    public int executeOutgoing(CommandContext<CommandSource> context) {
        return this.execute(context, false);
    }

    public int execute(CommandContext<CommandSource> context, boolean incoming) {
        Player player = (Player) context.getSource();

        var massDenyResponseFuture = this.friendService.massDenyFriendRequest(
                RelationshipProto.MassDenyFriendRequestRequest.newBuilder()
                        .setIssuerId(player.getUniqueId().toString())
                        .setIncoming(incoming)
                        .build()
        );

        Futures.addCallback(massDenyResponseFuture, FunctionalFutureCallback.create(
                result -> {
                    player.sendMessage(MINI_MESSAGE.deserialize(
                            incoming ? PURGED_INCOMING_MESSAGE : PURGED_OUTGOING_MESSAGE,
                            Placeholder.parsed("count", String.valueOf(result.getRequestsDenied()))
                    ));
                },
                error -> {
                    LOGGER.error("Failed to purge friend requests", error);
                    player.sendMessage(Component.text("Failed to purge friend requests", NamedTextColor.RED));
                }
        ), ForkJoinPool.commonPool());

        return 1;
    }
}
