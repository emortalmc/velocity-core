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

public class PartyJoinSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyJoinSub.class);

    private static final Metadata.Key<PartyProto.JoinPartyErrorResponse> JOIN_PARTY_ERROR_KEY = ProtoUtils.keyForProto(PartyProto.JoinPartyErrorResponse.getDefaultInstance());

    private final McPlayerGrpc.McPlayerFutureStub mcPlayerService = GrpcStubCollection.getPlayerService().orElse(null);
    private final PartyServiceGrpc.PartyServiceFutureStub partyService = GrpcStubCollection.getPartyService().orElse(null);

    public int execute(CommandContext<CommandSource> context) {
        Player executor = (Player) context.getSource();
        String targetUsername = context.getArgument("player", String.class);

        var playerResponseFuture = this.mcPlayerService.getPlayerByUsername(McPlayerProto.PlayerUsernameRequest.newBuilder().setUsername(targetUsername).build());
        Futures.addCallback(playerResponseFuture, FunctionalFutureCallback.create(
                playerResponse -> {
                    McPlayer targetPlayer = playerResponse.getPlayer();

                    var joinResponseFuture = this.partyService.joinParty(
                            PartyProto.JoinPartyRequest.newBuilder()
                                    .setPlayerId(executor.getUniqueId().toString())
                                    .setPlayerUsername(executor.getUsername())
                                    .setMemberId(targetPlayer.getId()).build()
                    );

                    Futures.addCallback(joinResponseFuture, FunctionalFutureCallback.create(
                            joinResponse -> executor.sendMessage(Component.text("Joined " + targetPlayer.getCurrentUsername() + "'s party", NamedTextColor.GREEN)),
                            throwable -> {
                                Metadata metadata = Status.trailersFromThrowable(throwable);
                                PartyProto.JoinPartyErrorResponse errorResponse = metadata.get(JOIN_PARTY_ERROR_KEY);
                                if (errorResponse == null) {
                                    LOGGER.error("An error occurred PartyJoinSub joinParty: ", throwable);
                                    executor.sendMessage(Component.text("An error occurred", NamedTextColor.RED));
                                    return;
                                }

                                executor.sendMessage(switch (errorResponse.getErrorType()) {
                                    case NOT_INVITED ->
                                            Component.text("You are not invited to this party", NamedTextColor.RED);
                                    case ALREADY_IN_PARTY ->
                                            Component.text("You are already in a party", NamedTextColor.RED);
                                    case PARTY_NOT_FOUND ->
                                            Component.text(targetPlayer.getCurrentUsername() + " is not in a party", NamedTextColor.RED);
                                    default -> {
                                        LOGGER.error("An error occurred PartyJoinSub joinParty: ", throwable);
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

                    LOGGER.error("An error occurred PartyJoinSub getPlayerByUsername: ", throwable);
                    executor.sendMessage(Component.text("An error occurred", NamedTextColor.RED));
                }
        ), ForkJoinPool.commonPool());
        return 1;
    }
}
