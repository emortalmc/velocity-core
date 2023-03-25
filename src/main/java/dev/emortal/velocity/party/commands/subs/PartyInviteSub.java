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
import dev.emortal.api.utils.resolvers.PlayerResolver;
import dev.emortal.velocity.lang.TempLang;
import dev.emortal.velocity.party.commands.PartyCommand;
import io.grpc.Status;
import io.grpc.protobuf.StatusProto;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;

public class PartyInviteSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyInviteSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();


    private static final String INVITED_MESSAGE = "<green>Invited <username> to the party";
    private static final String NO_PERMISSION_MESSAGE = "<red>You must be the leader of the party to invite another player";
    private static final String ALREADY_INVITED_MESSAGE = "<red><username> has already been invited to your party";
    private static final String ALREADY_IN_PARTY_MESSAGE = "<red><username> is already in the party";
    private static final String ALREADY_IN_PARTY_OTHER_MESSAGE = "<red><username> is in another party";
    private static final String PARTY_IS_OPEN_MESSAGE = "<red>The party is open, anyone can join";


    private final PartyServiceGrpc.PartyServiceFutureStub partyService = GrpcStubCollection.getPartyService().orElse(null);

    public int execute(CommandContext<CommandSource> context) {
        String targetUsername = context.getArgument("player", String.class);
        Player executor = (Player) context.getSource();

        PlayerResolver.retrievePlayerData(targetUsername, target -> {
                    if (!target.online()) {
                        TempLang.PLAYER_NOT_ONLINE.send(executor, Placeholder.unparsed("username", target.username()));
                        return;
                    }

                    var inviteResponseFuture = this.partyService.invitePlayer(
                            PartyProto.InvitePlayerRequest.newBuilder()
                                    .setIssuerId(executor.getUniqueId().toString())
                                    .setIssuerUsername(executor.getUsername())
                                    .setTargetId(target.uuid().toString())
                                    .setTargetUsername(target.username())
                                    .build()
                    );

                    Futures.addCallback(inviteResponseFuture, FunctionalFutureCallback.create(
                            inviteResponse -> executor.sendMessage(MINI_MESSAGE.deserialize(INVITED_MESSAGE, Placeholder.unparsed("username", target.username()))),
                            throwable -> {
                                com.google.rpc.Status status = StatusProto.fromThrowable(throwable);
                                if (status == null || status.getDetailsCount() == 0) {
                                    LOGGER.error("An error occurred PartyInviteSub invitePlayerToParty: ", throwable);
                                    executor.sendMessage(PartyCommand.ERROR_MESSAGE);
                                    return;
                                }

                                try {
                                    PartyProto.InvitePlayerErrorResponse errorResponse = status.getDetails(0).unpack(PartyProto.InvitePlayerErrorResponse.class);

                                    executor.sendMessage(switch (errorResponse.getErrorType()) {
                                        case NO_PERMISSION -> MINI_MESSAGE.deserialize(NO_PERMISSION_MESSAGE);
                                        case TARGET_ALREADY_INVITED -> MINI_MESSAGE.deserialize(ALREADY_INVITED_MESSAGE, Placeholder.unparsed("username", target.username()));
                                        case TARGET_ALREADY_IN_SELF_PARTY -> MINI_MESSAGE.deserialize(ALREADY_IN_PARTY_MESSAGE, Placeholder.unparsed("username", target.username()));
                                        // TODO: Why is this an error?
                                        case TARGET_ALREADY_IN_ANOTHER_PARTY -> MINI_MESSAGE.deserialize(ALREADY_IN_PARTY_OTHER_MESSAGE, Placeholder.unparsed("username", target.username()));
                                        // TODO: This too.
                                        case PARTY_IS_OPEN -> MINI_MESSAGE.deserialize(PARTY_IS_OPEN_MESSAGE);
                                        default -> {
                                            LOGGER.error("An error occurred PartyInviteSub invitePlayerToParty: ", throwable);
                                            yield PartyCommand.ERROR_MESSAGE;
                                        }
                                    });
                                } catch (InvalidProtocolBufferException e) {
                                    LOGGER.error("An error occurred PartyInviteSub invitePlayerToParty: ", throwable);
                                    executor.sendMessage(PartyCommand.ERROR_MESSAGE);
                                }
                            }
                    ), ForkJoinPool.commonPool());
                },
                status -> {
                    if (status.getCode() == Status.Code.NOT_FOUND) {
                        TempLang.PLAYER_NOT_FOUND.send(executor, Placeholder.unparsed("search_username", targetUsername));
                        return;
                    }

                    LOGGER.error("An error occurred PartyInviteSub getPlayerByUsername: ", status.asException());
                    executor.sendMessage(PartyCommand.ERROR_MESSAGE);
                }
        );

        return 1;
    }
}
