package dev.emortal.velocity.party.commands.subs;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.command.CommandExecutor;
import dev.emortal.api.model.party.PartyInvite;
import dev.emortal.api.service.party.InvitePlayerToPartyResult;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.api.utils.resolvers.PlayerResolver;
import dev.emortal.velocity.lang.TempLang;
import dev.emortal.velocity.party.commands.PartyCommand;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PartyInviteSub implements CommandExecutor<CommandSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyInviteSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String NO_PERMISSION_MESSAGE = "<red>You must be the leader of the party to invite another player";
    private static final String ALREADY_INVITED_MESSAGE = "<red><username> has already been invited to your party";
    private static final String ALREADY_IN_PARTY_MESSAGE = "<red><username> is already in the party";

    private final @NotNull PartyService partyService;

    public PartyInviteSub(@NotNull PartyService partyService) {
        this.partyService = partyService;
    }

    @Override
    public void execute(@NotNull CommandContext<CommandSource> context) {
        Player player = (Player) context.getSource();
        String targetUsername = context.getArgument("player", String.class);

        PlayerResolver.CachedMcPlayer target;
        try {
            target = PlayerResolver.getPlayerData(targetUsername);
        } catch (StatusException exception) {
            LOGGER.error("An error occurred PartyInviteSub getPlayerByUsername: ", exception);
            player.sendMessage(PartyCommand.ERROR_MESSAGE);
            return;
        }

        if (target == null) {
            TempLang.PLAYER_NOT_FOUND.send(player, Placeholder.unparsed("search_username", targetUsername));
            return;
        }

        if (!target.online()) {
            TempLang.PLAYER_NOT_ONLINE.send(player, Placeholder.unparsed("username", target.username()));
            return;
        }

        InvitePlayerToPartyResult result;
        try {
            result = this.partyService.invitePlayer(player.getUniqueId(), player.getUsername(), target.uuid(), target.username());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("An error occurred PartyInviteSub invitePlayerToParty: ", exception);
            player.sendMessage(PartyCommand.ERROR_MESSAGE);
            return;
        }

        switch (result) {
            case InvitePlayerToPartyResult.Success(PartyInvite ignored) -> {} // do nothing as we listen for the Kafka message
            case InvitePlayerToPartyResult.Error error -> {
                var message = switch (error) {
                    case NO_PERMISSION -> MINI_MESSAGE.deserialize(NO_PERMISSION_MESSAGE);
                    case TARGET_ALREADY_INVITED -> MINI_MESSAGE.deserialize(ALREADY_INVITED_MESSAGE, Placeholder.unparsed("username", target.username()));
                    case TARGET_IN_THIS_PARTY -> MINI_MESSAGE.deserialize(ALREADY_IN_PARTY_MESSAGE, Placeholder.unparsed("username", target.username()));
                };
                player.sendMessage(message);
            }
        }
    }
}
