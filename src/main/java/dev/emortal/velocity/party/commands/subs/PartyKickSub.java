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

public class PartyKickSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyKickSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final PartyServiceGrpc.PartyServiceFutureStub partyService = GrpcStubCollection.getPartyService().orElse(null);

    public int execute(CommandContext<CommandSource> context) {
        Player executor = (Player) context.getSource();
        String targetUsername = context.getArgument("player", String.class);

        PlayerResolver.retrievePlayerData(targetUsername,
                targetPlayer -> {
                    var kickFuture = this.partyService.kickPlayer(PartyProto.KickPlayerRequest.newBuilder()
                            .setIssuerId(executor.getUniqueId().toString())
                            .setIssuerUsername(executor.getUsername())
                            .setTargetId(targetPlayer.uuid().toString())
                            .build());

                    Futures.addCallback(kickFuture, FunctionalFutureCallback.create(
                            kickResponse -> executor.sendMessage(MINI_MESSAGE.deserialize("<green>Kicked " + targetPlayer.username() + " from your party")),
                            throwable -> {
                                Status status = StatusProto.fromThrowable(throwable);
                                if (status == null) {
                                    LOGGER.error("An error occurred PartyKickSub kickPlayerFromParty: ", throwable);
                                    executor.sendMessage(MINI_MESSAGE.deserialize("<red>An error occurred kicking " + targetPlayer.username() + " from your party"));
                                    return;
                                }

                                try {
                                    PartyProto.KickPlayerErrorResponse errorResponse = status.getDetails(0).unpack(PartyProto.KickPlayerErrorResponse.class);

                                    switch (errorResponse.getErrorType()) {
                                        case SELF_NOT_LEADER -> {
                                            executor.sendMessage(MINI_MESSAGE.deserialize("<red>You must be the party leader to kick players"));
                                        }
                                        case TARGET_IS_LEADER -> {
                                            executor.sendMessage(MINI_MESSAGE.deserialize("<red>You cannot kick the party leader"));
                                        }
                                        case SELF_NOT_IN_PARTY -> {
                                            executor.sendMessage(MINI_MESSAGE.deserialize("<red>You are not in a party"));
                                        }
                                        case TARGET_NOT_IN_PARTY -> {
                                            executor.sendMessage(MINI_MESSAGE.deserialize("<red>" + targetPlayer.username() + " is not in your party"));
                                        }
                                        default -> {
                                            LOGGER.error("An error occurred PartyKickSub kickPlayerFromParty: ", throwable);
                                            executor.sendMessage(MINI_MESSAGE.deserialize("<red>An error occurred kicking " + targetPlayer.username() + " from your party"));
                                        }
                                    }
                                } catch (InvalidProtocolBufferException e) {
                                    LOGGER.error("An error occurred PartyKickSub kickPlayerFromParty: ", throwable);
                                    executor.sendMessage(MINI_MESSAGE.deserialize("<red>An error occurred kicking " + targetPlayer.username() + " from your party"));
                                }
                            }
                    ), ForkJoinPool.commonPool());
                },
                status -> {
                    if (status == io.grpc.Status.NOT_FOUND) {
                        TempLang.PLAYER_NOT_FOUND.send(executor, Placeholder.unparsed("search_username", targetUsername));
                        return;
                    }

                    LOGGER.error("An error occurred PartyKickSub getPlayerByUsername: ", status.asException());
                    executor.sendMessage(MINI_MESSAGE.deserialize("<red>Could not find player " + targetUsername));
                }
        );

        return 1;
    }
}
