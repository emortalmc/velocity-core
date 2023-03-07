package dev.emortal.velocity.party.commands.subs;

import com.google.common.util.concurrent.Futures;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Status;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.grpc.party.PartyProto;
import dev.emortal.api.grpc.party.PartyServiceGrpc;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import dev.emortal.api.utils.resolvers.PlayerResolver;
import dev.emortal.velocity.lang.TempLang;
import io.grpc.protobuf.StatusProto;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;

public class PartyJoinSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyJoinSub.class);

    private final PartyServiceGrpc.PartyServiceFutureStub partyService = GrpcStubCollection.getPartyService().orElse(null);

    public int execute(CommandContext<CommandSource> context) {
        Player executor = (Player) context.getSource();
        String targetUsername = context.getArgument("player", String.class);

        PlayerResolver.retrievePlayerData(targetUsername,
                targetPlayer -> {
                    var joinResponseFuture = this.partyService.joinParty(
                            PartyProto.JoinPartyRequest.newBuilder()
                                    .setPlayerId(executor.getUniqueId().toString())
                                    .setPlayerUsername(executor.getUsername())
                                    .setMemberId(targetPlayer.uuid().toString()).build()
                    );

                    Futures.addCallback(joinResponseFuture, FunctionalFutureCallback.create(
                            joinResponse -> executor.sendMessage(Component.text("Joined " + targetPlayer.username() + "'s party", NamedTextColor.GREEN)),
                            throwable -> {
                                Status status = StatusProto.fromThrowable(throwable);
                                if (status == null) {
                                    LOGGER.error("An error occurred PartyJoinSub joinParty: ", throwable);
                                    executor.sendMessage(Component.text("An error occurred", NamedTextColor.RED));
                                    return;
                                }

                                try {
                                    PartyProto.JoinPartyErrorResponse errorResponse = status.getDetails(0).unpack(PartyProto.JoinPartyErrorResponse.class);

                                    executor.sendMessage(switch (errorResponse.getErrorType()) {
                                        case NOT_INVITED -> Component.text("You are not invited to this party", NamedTextColor.RED);
                                        case ALREADY_IN_PARTY -> Component.text("You are already in a party", NamedTextColor.RED);
                                        case PARTY_NOT_FOUND -> Component.text(targetPlayer.username() + " is not in a party", NamedTextColor.RED);
                                        default -> {
                                            LOGGER.error("An error occurred PartyJoinSub joinParty: ", throwable);
                                            yield Component.text("An error occurred", NamedTextColor.RED);
                                        }
                                    });
                                } catch (InvalidProtocolBufferException e) {
                                    LOGGER.error("An error occurred PartyJoinSub joinParty: ", throwable);
                                    executor.sendMessage(Component.text("An error occurred", NamedTextColor.RED));
                                }
                            }
                    ), ForkJoinPool.commonPool());
                },
                status -> {
                    if (status == io.grpc.Status.NOT_FOUND) {
                        TempLang.PLAYER_NOT_FOUND.send(executor, Placeholder.unparsed("search_username", targetUsername));
                        return;
                    }

                    LOGGER.error("An error occurred PartyJoinSub getPlayerByUsername: ", status.asException());
                    executor.sendMessage(Component.text("An error occurred", NamedTextColor.RED));
                }
        );
        return 1;
    }
}
