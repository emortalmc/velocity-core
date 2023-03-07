package dev.emortal.velocity.party.commands.subs;

import com.google.common.util.concurrent.Futures;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Code;
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

public class PartyCreateSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyCreateSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final PartyServiceGrpc.PartyServiceFutureStub partyService = GrpcStubCollection.getPartyService().orElse(null);

    public int execute(CommandContext<CommandSource> context) {
        // context is ignored
        Player executor = (Player) context.getSource();

        var createPartyFuture = this.partyService.createParty(PartyProto.CreatePartyRequest.newBuilder()
                .setOwnerId(executor.getUniqueId().toString())
                .setOwnerUsername(executor.getUsername())
                .build());

        Futures.addCallback(createPartyFuture, FunctionalFutureCallback.create(
                response -> {
                    executor.sendMessage(MINI_MESSAGE.deserialize("<green>Party created"));
                },
                throwable -> {
                    Status status = StatusProto.fromThrowable(throwable);
                    if (status == null) {
                        LOGGER.error("Failed to create party", throwable);
                        executor.sendMessage(MINI_MESSAGE.deserialize("<red>Failed to create party"));
                        return;
                    }

                    if (status.getCode() == Code.ALREADY_EXISTS_VALUE) {
                        executor.sendMessage(MINI_MESSAGE.deserialize("<red>You are already in a party."));
                        return;
                    }

                    if (status.getDetailsCount() == 0) {
                        LOGGER.error("Failed to create party (details count: {}) {}", status.getDetailsCount(), throwable);
                        executor.sendMessage(MINI_MESSAGE.deserialize("<red>Failed to create party"));
                        return;
                    }

                    try {
                        PartyProto.CreatePartyErrorResponse errorResponse = status.getDetails(0).unpack(PartyProto.CreatePartyErrorResponse.class);

                        switch (errorResponse.getErrorType()) {
                            case ALREADY_IN_PARTY -> executor.sendMessage(MINI_MESSAGE.deserialize("<red>You are already in a party"));
                            default -> {
                                LOGGER.error("Failed to create party", throwable);
                                executor.sendMessage(MINI_MESSAGE.deserialize("<red>Failed to create party"));
                            }
                        }
                    } catch (InvalidProtocolBufferException e) {
                        LOGGER.error("Failed to create party", throwable);
                        executor.sendMessage(MINI_MESSAGE.deserialize("<red>Failed to create party"));
                    }
                }
        ), ForkJoinPool.commonPool());

        return 1;
    }
}
