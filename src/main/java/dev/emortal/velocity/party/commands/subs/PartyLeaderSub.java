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
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;

// TODO
public class PartyLeaderSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyLeaderSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();


    private static final String UPDATED_LEADER_MESSAGE = "<green>Successfully updated the party leader";
    private static final String NOT_LEADER_MESSAGE = "<red>You must be the party leader to update the party leader";
    private static final String NOT_IN_PARTY_MESSAGE = "<red><username> is not in your party";


    private final PartyServiceGrpc.PartyServiceFutureStub partyService = GrpcStubCollection.getPartyService().orElse(null);

    public int execute(CommandContext<CommandSource> context) {
        String targetUsername = context.getArgument("player", String.class);
        Player executor = (Player) context.getSource();

        PlayerResolver.retrievePlayerData(targetUsername,
                target -> {
                    var setLeaderRequestFuture = this.partyService.setPartyLeader(PartyProto.SetPartyLeaderRequest.newBuilder()
                            .setIssuerId(executor.getUniqueId().toString())
                            .setIssuerUsername(executor.getUsername())
                            .setTargetId(target.uuid().toString())
                            .build());

                    Futures.addCallback(setLeaderRequestFuture, FunctionalFutureCallback.create(
                            response -> executor.sendMessage(MINI_MESSAGE.deserialize(UPDATED_LEADER_MESSAGE)),
                            throwable -> {
                                Status status = StatusProto.fromThrowable(throwable);
                                if (status == null || status.getDetailsCount() == 0) {
                                    LOGGER.error("An error occurred PartyLeaderSub setPartyLeader: ", throwable);
                                    executor.sendMessage(PartyCommand.ERROR_MESSAGE);
                                    return;
                                }

                                try {
                                    PartyProto.SetPartyLeaderErrorResponse errorResponse = status.getDetails(0).unpack(PartyProto.SetPartyLeaderErrorResponse.class);

                                    executor.sendMessage(switch (errorResponse.getErrorType()) {
                                        case SELF_NOT_LEADER -> MINI_MESSAGE.deserialize(NOT_LEADER_MESSAGE);
                                        case TARGET_NOT_IN_PARTY -> MINI_MESSAGE.deserialize(NOT_IN_PARTY_MESSAGE, Placeholder.unparsed("username", target.username()));
                                        default -> {
                                            LOGGER.error("An error occurred PartyLeaderSub setPartyLeader: ", throwable);
                                            yield PartyCommand.ERROR_MESSAGE;
                                        }
                                    });
                                } catch (InvalidProtocolBufferException e) {
                                    LOGGER.error("An error occurred PartyLeaderSub setPartyLeader: ", e);
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

                    LOGGER.error("An error occurred PartyLeaderSub retrievePlayerData: {}", status);
                    executor.sendMessage(PartyCommand.ERROR_MESSAGE);
                });
        return 1;
    }
}
