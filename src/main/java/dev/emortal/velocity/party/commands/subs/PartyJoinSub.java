package dev.emortal.velocity.party.commands.subs;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.command.CommandExecutor;
import dev.emortal.api.model.party.Party;
import dev.emortal.api.service.party.JoinPartyResult;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.api.utils.resolvers.PlayerResolver;
import dev.emortal.velocity.lang.TempLang;
import dev.emortal.velocity.party.commands.PartyCommand;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PartyJoinSub implements CommandExecutor<CommandSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyJoinSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();


    private static final String PARTY_JOIN_MESSAGE = "<green>Joined <username>'s party";
    private static final Component NOT_INVITED_MESSAGE = MINI_MESSAGE.deserialize("<red>You were not invited to this party");
    private static final Component ALREADY_IN_PARTY_MESSAGE = MINI_MESSAGE.deserialize("<red>You are already in the party");


    private final @NotNull PartyService partyService;

    public PartyJoinSub(@NotNull PartyService partyService) {
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
            LOGGER.error("An error occurred PartyJoinSub getPlayerByUsername: ", exception);
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

        JoinPartyResult result;
        try {
            result = this.partyService.joinParty(player.getUniqueId(), player.getUsername(), target.uuid());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("An error occurred PartyJoinSub joinParty: ", exception);
            player.sendMessage(PartyCommand.ERROR_MESSAGE);
            return;
        }

        switch (result) {
            case JoinPartyResult.Success(Party party) -> {
                var username = Placeholder.unparsed("username", target.username());
                player.sendMessage(MINI_MESSAGE.deserialize(PARTY_JOIN_MESSAGE, username));
            }
            case JoinPartyResult.Error error -> {
                var message = switch (error) {
                    case ALREADY_IN_PARTY -> ALREADY_IN_PARTY_MESSAGE;
                    case NOT_INVITED -> NOT_INVITED_MESSAGE;
                };
                player.sendMessage(message);
            }
        }
    }
}
