package dev.emortal.velocity.party.commands.subs;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.model.party.PartyInvite;
import dev.emortal.api.service.party.InvitePlayerToPartyResult;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.api.utils.resolvers.PlayerResolver;
import dev.emortal.velocity.lang.TempLang;
import dev.emortal.velocity.party.commands.PartyCommand;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PartyInviteSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyInviteSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String NO_PERMISSION_MESSAGE = "<red>You must be the leader of the party to invite another player";
    private static final String ALREADY_INVITED_MESSAGE = "<red><username> has already been invited to your party";
    private static final String ALREADY_IN_PARTY_MESSAGE = "<red><username> is already in the party";
    private static final String ALREADY_IN_PARTY_OTHER_MESSAGE = "<red><username> is in another party";
    private static final String PARTY_IS_OPEN_MESSAGE = "<red>The party is open, anyone can join";


    private final @NotNull PartyService partyService;

    public PartyInviteSub(@NotNull PartyService partyService) {
        this.partyService = partyService;
    }

    public void execute(CommandContext<CommandSource> context) {
        String targetUsername = context.getArgument("player", String.class);
        Player executor = (Player) context.getSource();

        PlayerResolver.CachedMcPlayer target;
        try {
            target = PlayerResolver.getPlayerData(targetUsername);
        } catch (StatusException exception) {
            Status status = exception.getStatus();
            if (status.getCode() == Status.Code.NOT_FOUND) {
                TempLang.PLAYER_NOT_FOUND.send(executor, Placeholder.unparsed("search_username", targetUsername));
                return;
            }

            LOGGER.error("An error occurred PartyInviteSub getPlayerByUsername: ", status.asException());
            executor.sendMessage(PartyCommand.ERROR_MESSAGE);
            return;
        }

        if (!target.online()) {
            TempLang.PLAYER_NOT_ONLINE.send(executor, Placeholder.unparsed("username", target.username()));
            return;
        }

        InvitePlayerToPartyResult result;
        try {
            result = this.partyService.invitePlayer(executor.getUniqueId(), executor.getUsername(), target.uuid(), target.username());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("An error occurred PartyInviteSub invitePlayerToParty: ", exception);
            executor.sendMessage(PartyCommand.ERROR_MESSAGE);
            return;
        }

        switch (result) {
            case InvitePlayerToPartyResult.Success(PartyInvite invite) -> {} // do nothing as we listen for the Kafka message
            case InvitePlayerToPartyResult.Error error -> {
                var message = switch (error) {
                    case NO_PERMISSION -> MINI_MESSAGE.deserialize(NO_PERMISSION_MESSAGE);
                    case TARGET_ALREADY_INVITED -> MINI_MESSAGE.deserialize(ALREADY_INVITED_MESSAGE, Placeholder.unparsed("username", target.username()));
                    case TARGET_IN_THIS_PARTY -> MINI_MESSAGE.deserialize(ALREADY_IN_PARTY_MESSAGE, Placeholder.unparsed("username", target.username()));
                    // TODO: Why is this an error?
                    case TARGET_IN_OTHER_PARTY -> MINI_MESSAGE.deserialize(ALREADY_IN_PARTY_OTHER_MESSAGE, Placeholder.unparsed("username", target.username()));
                    // TODO: This too.
                    case PARTY_OPEN -> MINI_MESSAGE.deserialize(PARTY_IS_OPEN_MESSAGE);
                };
                executor.sendMessage(message);
            }
        }
    }
}
