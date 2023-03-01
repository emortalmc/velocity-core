package dev.emortal.velocity.party.commands.subs;

import com.google.common.util.concurrent.Futures;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.grpc.mcplayer.McPlayerGrpc;
import dev.emortal.api.grpc.mcplayer.McPlayerProto;
import dev.emortal.api.grpc.party.PartyProto;
import dev.emortal.api.grpc.party.PartyServiceGrpc;
import dev.emortal.api.model.mcplayer.McPlayer;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.protobuf.ProtoUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;

public class PartyInviteSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyInviteSub.class);

    private static final Metadata.Key<PartyProto.InvitePlayerErrorResponse> INVITE_ERROR_METADATA_KEY =
            ProtoUtils.keyForProto(PartyProto.InvitePlayerErrorResponse.getDefaultInstance());

    private final McPlayerGrpc.McPlayerFutureStub mcPlayerService = GrpcStubCollection.getPlayerService().orElse(null);
    private final PartyServiceGrpc.PartyServiceFutureStub partyService = GrpcStubCollection.getPartyService().orElse(null);

    public int execute(CommandContext<CommandSource> context) {
        String targetUsername = context.getArgument("player", String.class);
        Player executor = (Player) context.getSource();

        var playerResponseFuture = this.mcPlayerService.getPlayerByUsername(McPlayerProto.PlayerUsernameRequest.newBuilder()
                .setUsername(targetUsername).build());

        Futures.addCallback(playerResponseFuture, FunctionalFutureCallback.create(
                playerResponse -> {
                    McPlayer target = playerResponse.getPlayer();

                    var inviteResponseFuture = this.partyService.invitePlayer(
                            PartyProto.InvitePlayerRequest.newBuilder()
                                    .setIssuerId(executor.getUniqueId().toString())
                                    .setIssuerUsername(executor.getUsername())
                                    .setTargetId(target.getId())
                                    .setTargetUsername(target.getCurrentUsername())
                                    .build()
                    );

                    Futures.addCallback(inviteResponseFuture, FunctionalFutureCallback.create(
                            inviteResponse -> executor.sendMessage(Component.text("Invited " + target.getCurrentUsername() + " to your party", NamedTextColor.GREEN)),
                            throwable -> {
                                Metadata metadata = Status.trailersFromThrowable(throwable);
                                PartyProto.InvitePlayerErrorResponse errorResponse = metadata.get(INVITE_ERROR_METADATA_KEY);
                                if (errorResponse == null) {
                                    LOGGER.error("An error occurred PartyInviteSub invitePlayerToParty: ", throwable);
                                    executor.sendMessage(Component.text("An error occurred", NamedTextColor.RED));
                                    return;
                                }

                                executor.sendMessage(switch (errorResponse.getErrorType()) {
                                    case SELF_NOT_LEADER -> Component.text("You must be the leader of the party to invite another player.", NamedTextColor.RED);
                                    case SELF_NOT_IN_PARTY -> Component.text("You are not in a party.", NamedTextColor.RED);
                                    case TARGET_ALREADY_INVITED -> Component.text(target.getCurrentUsername() + " is already invited to your party.", NamedTextColor.RED);
                                    case TARGET_ALREADY_IN_SELF_PARTY -> Component.text(target.getCurrentUsername() + " is already in your party.", NamedTextColor.RED);
                                    case TARGET_ALREADY_IN_ANOTHER_PARTY -> Component.text(target.getCurrentUsername() + " is already in another party.", NamedTextColor.RED);
                                    default -> {
                                        LOGGER.error("An error occurred PartyInviteSub invitePlayerToParty: ", throwable);
                                        yield Component.text("An error occurred", NamedTextColor.RED);
                                    }
                                });
                            }
                    ), ForkJoinPool.commonPool());
                },
                throwable -> {
                    Status status = Status.fromThrowable(throwable);
                    if (status.getCode() == Status.Code.NOT_FOUND) {
                        executor.sendMessage(Component.text("Player not found", NamedTextColor.RED));
                        return;
                    }

                    LOGGER.error("An error occurred PartyInviteSub getPlayerByUsername: ", throwable);
                    executor.sendMessage(Component.text("An error occurred", NamedTextColor.RED));
                }
        ), ForkJoinPool.commonPool());

        return 1;
    }
}
