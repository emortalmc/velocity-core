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
import io.grpc.Status;
import io.grpc.protobuf.StatusProto;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;

public class PartyInviteSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyInviteSub.class);

    private final PartyServiceGrpc.PartyServiceFutureStub partyService = GrpcStubCollection.getPartyService().orElse(null);

    public int execute(CommandContext<CommandSource> context) {
        String targetUsername = context.getArgument("player", String.class);
        Player executor = (Player) context.getSource();

        PlayerResolver.retrievePlayerData(targetUsername, target -> {
                    var inviteResponseFuture = this.partyService.invitePlayer(
                            PartyProto.InvitePlayerRequest.newBuilder()
                                    .setIssuerId(executor.getUniqueId().toString())
                                    .setIssuerUsername(executor.getUsername())
                                    .setTargetId(target.uuid().toString())
                                    .setTargetUsername(target.username())
                                    .build()
                    );

                    Futures.addCallback(inviteResponseFuture, FunctionalFutureCallback.create(
                            inviteResponse -> executor.sendMessage(Component.text("Invited " + target.username() + " to your party", NamedTextColor.GREEN)),
                            throwable -> {
                                com.google.rpc.Status status = StatusProto.fromThrowable(throwable);
                                if (status == null) {
                                    LOGGER.error("An error occurred PartyInviteSub invitePlayerToParty: ", throwable);
                                    executor.sendMessage(Component.text("An error occurred inviting " + target.username(), NamedTextColor.RED));
                                    return;
                                }

                                try {
                                    PartyProto.InvitePlayerErrorResponse errorResponse = status.getDetails(0).unpack(PartyProto.InvitePlayerErrorResponse.class);

                                    executor.sendMessage(switch (errorResponse.getErrorType()) {
                                        case NO_PERMISSION ->
                                                Component.text("You must be the leader of the party to invite another player.", NamedTextColor.RED);
                                        case TARGET_ALREADY_INVITED ->
                                                Component.text(target.username() + " is already invited to your party.", NamedTextColor.RED);
                                        case TARGET_ALREADY_IN_SELF_PARTY ->
                                                Component.text(target.username() + " is already in your party.", NamedTextColor.RED);
                                        case TARGET_ALREADY_IN_ANOTHER_PARTY ->
                                                Component.text(target.username() + " is already in another party.", NamedTextColor.RED);
                                        default -> {
                                            LOGGER.error("An error occurred PartyInviteSub invitePlayerToParty: ", throwable);
                                            yield Component.text("An error occurred", NamedTextColor.RED);
                                        }
                                    });
                                } catch (InvalidProtocolBufferException e) {
                                    LOGGER.error("An error occurred PartyInviteSub invitePlayerToParty: ", throwable);
                                    executor.sendMessage(Component.text("An error occurred", NamedTextColor.RED));
                                }
                            }
                    ), ForkJoinPool.commonPool());
                },
                status -> {
                    if (status == Status.NOT_FOUND) {
                        TempLang.PLAYER_NOT_FOUND.send(executor, Placeholder.unparsed("search_username", targetUsername));
                        return;
                    }

                    LOGGER.error("An error occurred PartyInviteSub getPlayerByUsername: ", status.asException());
                    executor.sendMessage(Component.text("An error occurred", NamedTextColor.RED));
                }
        );

        return 1;
    }
}
