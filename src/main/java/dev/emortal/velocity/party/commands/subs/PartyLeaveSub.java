package dev.emortal.velocity.party.commands.subs;

import com.google.common.util.concurrent.Futures;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.grpc.party.PartyProto;
import dev.emortal.api.grpc.party.PartyServiceGrpc;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import dev.emortal.velocity.party.commands.PartyCommand;
import io.grpc.protobuf.StatusProto;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;

public class PartyLeaveSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyLeaveSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();


    private static final Component LEFT_MESSAGE = MINI_MESSAGE.deserialize("<green>Left party");
    private static final Component LEFT_AS_LEADER_MESSAGE = MINI_MESSAGE.deserialize("""
            <red>You are the leader of the party
            <red>Use /party disband to disband the party or /party leader <player> to transfer leadership""");


    private final PartyServiceGrpc.PartyServiceFutureStub partyService = GrpcStubCollection.getPartyService().orElse(null);

    public int execute(CommandContext<CommandSource> context) {
        // context is ignored
        Player executor = (Player) context.getSource();

        var leavePartyFuture = this.partyService.leaveParty(PartyProto.LeavePartyRequest.newBuilder()
                .setPlayerId(executor.getUniqueId().toString())
                .build());

        Futures.addCallback(leavePartyFuture, FunctionalFutureCallback.create(
                response -> {
                    executor.sendMessage(LEFT_MESSAGE);
                },
                throwable -> {
                    com.google.rpc.Status status = StatusProto.fromThrowable(throwable);
                    if (status == null || status.getDetailsCount() == 0) {
                        LOGGER.error("Failed to leave party", throwable);
                        executor.sendMessage(PartyCommand.ERROR_MESSAGE);
                        return;
                    }

                    try {
                        PartyProto.LeavePartyErrorResponse errorResponse = status.getDetails(0).unpack(PartyProto.LeavePartyErrorResponse.class);

                        executor.sendMessage(switch (errorResponse.getErrorType()) {
                            case CANNOT_LEAVE_AS_LEADER -> LEFT_AS_LEADER_MESSAGE;
                            default -> {
                                LOGGER.error("Failed to leave party", throwable);
                                yield PartyCommand.ERROR_MESSAGE;
                            }
                        });
                    } catch (InvalidProtocolBufferException e) {
                        LOGGER.error("Failed to leave party", throwable);
                        executor.sendMessage(PartyCommand.ERROR_MESSAGE);
                    }
                }
        ), ForkJoinPool.commonPool());

        return 1;
    }
}
