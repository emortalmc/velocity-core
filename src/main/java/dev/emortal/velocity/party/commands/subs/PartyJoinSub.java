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
import dev.emortal.velocity.party.commands.PartyCommand;
import io.grpc.protobuf.StatusProto;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;

public class PartyJoinSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyJoinSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();


    private static final String PARTY_JOIN_MESSAGE = "<green>Joined <username>'s party";
    private static final Component NOT_INVITED_MESSAGE = MINI_MESSAGE.deserialize("<red>You were not invited to this party");
    private static final Component ALREADY_IN_PARTY_MESSAGE = MINI_MESSAGE.deserialize("<red>You are already in the party");


    private final PartyServiceGrpc.PartyServiceFutureStub partyService = GrpcStubCollection.getPartyService().orElse(null);

    public int execute(CommandContext<CommandSource> context) {
        Player executor = (Player) context.getSource();
        String targetUsername = context.getArgument("player", String.class);

        PlayerResolver.retrievePlayerData(targetUsername,
                target -> {
                    if (!target.online()) {
                        TempLang.PLAYER_NOT_ONLINE.send(executor, Placeholder.unparsed("username", target.username()));
                        return;
                    }

                    var joinResponseFuture = this.partyService.joinParty(
                            PartyProto.JoinPartyRequest.newBuilder()
                                    .setPlayerId(executor.getUniqueId().toString())
                                    .setPlayerUsername(executor.getUsername())
                                    .setTargetPlayerId(target.uuid().toString()).build()
                    );

                    Futures.addCallback(joinResponseFuture, FunctionalFutureCallback.create(
                            joinResponse -> executor.sendMessage(MINI_MESSAGE.deserialize(PARTY_JOIN_MESSAGE, Placeholder.unparsed("username", target.username()))),
                            throwable -> {
                                Status status = StatusProto.fromThrowable(throwable);
                                if (status == null || status.getDetailsCount() == 0) {
                                    LOGGER.error("An error occurred PartyJoinSub joinParty: ", throwable);
                                    executor.sendMessage(PartyCommand.ERROR_MESSAGE);
                                    return;
                                }

                                try {
                                    PartyProto.JoinPartyErrorResponse errorResponse = status.getDetails(0).unpack(PartyProto.JoinPartyErrorResponse.class);

                                    executor.sendMessage(switch (errorResponse.getErrorType()) {
                                        case NOT_INVITED -> NOT_INVITED_MESSAGE;
                                        case ALREADY_IN_PARTY -> ALREADY_IN_PARTY_MESSAGE;
                                        default -> {
                                            LOGGER.error("An error occurred PartyJoinSub joinParty: ", throwable);
                                            yield PartyCommand.ERROR_MESSAGE;
                                        }
                                    });
                                } catch (InvalidProtocolBufferException e) {
                                    LOGGER.error("An error occurred PartyJoinSub joinParty: ", throwable);
                                    executor.sendMessage(PartyCommand.ERROR_MESSAGE);
                                }
                            }
                    ), ForkJoinPool.commonPool());
                },
                status -> {
                    if (status.getCode() == io.grpc.Status.Code.NOT_FOUND) {
                        TempLang.PLAYER_NOT_FOUND.send(executor, Placeholder.unparsed("search_username", targetUsername));
                        return;
                    }

                    LOGGER.error("An error occurred PartyJoinSub getPlayerByUsername: ", status.asException());
                    executor.sendMessage(PartyCommand.ERROR_MESSAGE);
                }
        );
        return 1;
    }
}
