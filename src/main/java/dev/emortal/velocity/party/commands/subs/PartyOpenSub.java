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
import dev.emortal.velocity.party.PartyCache;
import dev.emortal.velocity.party.commands.PartyCommand;
import io.grpc.protobuf.StatusProto;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;

public class PartyOpenSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyOpenSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();


    private static final Component PARTY_CLOSED_MESSAGE = MINI_MESSAGE.deserialize("<green>The party is now closed");
    private static final Component PARTY_OPENED_MESSAGE = MINI_MESSAGE.deserialize("<green>The party is now open");
    private static final Component NOT_LEADER_MESSAGE = MINI_MESSAGE.deserialize("<red>You are not the leader of the party");


    private final PartyServiceGrpc.PartyServiceFutureStub partyService = GrpcStubCollection.getPartyService().orElse(null);
    private final @NotNull PartyCache partyCache;

    public PartyOpenSub(@NotNull PartyCache partyCache) {
        this.partyCache = partyCache;
    }

    public int execute(CommandContext<CommandSource> context) {
        Player executor = (Player) context.getSource();
        PartyCache.CachedParty party = this.partyCache.getPlayerParty(executor.getUniqueId());
        if (party == null) {
            executor.sendMessage(PartyCommand.NOT_IN_PARTY_MESSAGE);
            return 1;
        }

        var openPartyFuture = this.partyService.setOpenParty(PartyProto.SetOpenPartyRequest.newBuilder()
                .setPlayerId(executor.getUniqueId().toString())
                .setOpen(!party.isOpen())
                .build());

        Futures.addCallback(openPartyFuture, FunctionalFutureCallback.create(
                response -> {
                    party.setOpen(!party.isOpen());

                    executor.sendMessage(party.isOpen() ? PARTY_OPENED_MESSAGE : PARTY_CLOSED_MESSAGE);
                },
                throwable -> {
                    Status status = StatusProto.fromThrowable(throwable);
                    if (status == null || status.getDetailsCount() == 0) {
                        LOGGER.error("Failed to open party", throwable);
                        executor.sendMessage(PartyCommand.ERROR_MESSAGE);
                        return;
                    }

                    try {
                        PartyProto.SetOpenPartyErrorResponse errorResponse = status.getDetails(0).unpack(PartyProto.SetOpenPartyErrorResponse.class);

                        executor.sendMessage(switch (errorResponse.getErrorType()) {
                            case NOT_LEADER -> NOT_LEADER_MESSAGE;
                            default -> {
                                LOGGER.error("Failed to open party", throwable);
                                yield PartyCommand.ERROR_MESSAGE;
                            }
                        });
                    } catch (InvalidProtocolBufferException e) {
                        LOGGER.error("Failed to open party", throwable);
                        executor.sendMessage(PartyCommand.ERROR_MESSAGE);
                    }
                }
        ), ForkJoinPool.commonPool());

        return 1;
    }
}
