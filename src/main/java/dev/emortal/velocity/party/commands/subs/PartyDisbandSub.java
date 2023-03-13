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
import io.grpc.protobuf.StatusProto;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;

public class PartyDisbandSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyInfoSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final PartyServiceGrpc.PartyServiceFutureStub partyService = GrpcStubCollection.getPartyService().orElse(null);

    public int execute(CommandContext<CommandSource> context) {
        // context is ignored
        Player executor = (Player) context.getSource();

        var disbandPartyFuture = this.partyService.emptyParty(PartyProto.EmptyPartyRequest.newBuilder()
                .setPlayerId(executor.getUniqueId().toString())
                .build());

        Futures.addCallback(disbandPartyFuture, FunctionalFutureCallback.create(
                response -> {
                    executor.sendMessage(MINI_MESSAGE.deserialize("<hover:show_text:'<color:#9fff87>Note: disbanding a party only empties it</color>'><green>Party disbanded</green></hover>"));
                },
                throwable -> {
                    Status status = StatusProto.fromThrowable(throwable);
                    if (status == null || status.getDetailsCount() == 0) {
                        LOGGER.error("Failed to disband party", throwable);
                        executor.sendMessage(MINI_MESSAGE.deserialize("<red>Failed to disband party"));
                        return;
                    }

                    try {
                        PartyProto.EmptyPartyErrorResponse errorResponse = status.getDetails(0).unpack(PartyProto.EmptyPartyErrorResponse.class);

                        executor.sendMessage(switch (errorResponse.getErrorType()) {
                            case NOT_LEADER -> MINI_MESSAGE.deserialize("""
                                    <red>You are not the leader of the party
                                    <red>Use /party leave to leave the party instead""");
                            default -> {
                                LOGGER.error("Failed to disband party", throwable);
                                yield MINI_MESSAGE.deserialize("<red>Failed to disband party");
                            }
                        });
                    } catch (InvalidProtocolBufferException e) {
                        LOGGER.error("Failed to disband party", throwable);
                        executor.sendMessage(MINI_MESSAGE.deserialize("<red>Failed to disband party"));
                    }
                }
        ), ForkJoinPool.commonPool());

        return 1;
    }
}
