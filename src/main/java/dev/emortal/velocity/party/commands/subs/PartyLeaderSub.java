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
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;

// TODO
public class PartyLeaderSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyLeaderSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final PartyServiceGrpc.PartyServiceFutureStub partyService = GrpcStubCollection.getPartyService().orElse(null);

    public int execute(CommandContext<CommandSource> context) {
        String targetUsername = context.getArgument("target", String.class);
        Player executor = (Player) context.getSource();

        PlayerResolver.retrievePlayerData(targetUsername,
                targetPlayer -> {
                    var setLeaderRequestFuture = this.partyService.setPartyLeader(PartyProto.SetPartyLeaderRequest.newBuilder()
                            .setIssuerId(executor.getUniqueId().toString())
                            .setIssuerUsername(executor.getUsername())
                            .setTargetId(targetPlayer.uuid().toString())
                            .build());

                    Futures.addCallback(setLeaderRequestFuture, FunctionalFutureCallback.create(
                            response -> executor.sendMessage(MINI_MESSAGE.deserialize("<green>Successfully updated the party leader")),
                            throwable -> {
                                Status status = StatusProto.fromThrowable(throwable);
                                if (status == null || status.getDetailsCount() == 0) {
                                    LOGGER.error("An error occurred PartyLeaderSub setPartyLeader: ", throwable);
                                    executor.sendMessage(MINI_MESSAGE.deserialize("<red>An error occurred updating the party leader"));
                                    return;
                                }

                                try {
                                    PartyProto.SetPartyLeaderErrorResponse errorResponse = status.getDetails(0).unpack(PartyProto.SetPartyLeaderErrorResponse.class);

                                    executor.sendMessage(switch (errorResponse.getErrorType()) {
                                        case SELF_NOT_LEADER ->
                                                MINI_MESSAGE.deserialize("<red>You must be the party leader to update the party leader");
                                        case SELF_NOT_IN_PARTY ->
                                                MINI_MESSAGE.deserialize("<red>You are not in a party");
                                        case TARGET_NOT_IN_PARTY ->
                                                MINI_MESSAGE.deserialize("<red>The target is not in your party");
                                        default -> {
                                            LOGGER.error("An error occurred PartyLeaderSub setPartyLeader: ", throwable);
                                            yield MINI_MESSAGE.deserialize("<red>An error occurred updating the party leader");
                                        }
                                    });
                                } catch (InvalidProtocolBufferException e) {
                                    LOGGER.error("An error occurred PartyLeaderSub setPartyLeader: ", e);
                                    executor.sendMessage(MINI_MESSAGE.deserialize("<red>An error occurred updating the party leader"));
                                }
                            }
                    ), ForkJoinPool.commonPool());

                },
                status -> {
                    if (status == io.grpc.Status.NOT_FOUND) {
                        TempLang.PLAYER_NOT_FOUND.send(executor, Placeholder.unparsed("search_username", targetUsername));
                        return;
                    }

                    LOGGER.error("An error occurred PartyLeaderSub retrievePlayerData: {}", status);
                    executor.sendMessage(MINI_MESSAGE.deserialize("<red>An error occurred updating the party leader"));
                });
        return 1;
    }
}
