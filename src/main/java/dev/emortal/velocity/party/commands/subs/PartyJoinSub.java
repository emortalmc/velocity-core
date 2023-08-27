package dev.emortal.velocity.party.commands.subs;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.command.CommandExecutor;
import dev.emortal.api.model.party.Party;
import dev.emortal.api.service.party.JoinPartyResult;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.api.utils.resolvers.PlayerResolver;
import dev.emortal.velocity.lang.ChatMessages;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PartyJoinSub implements CommandExecutor<CommandSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyJoinSub.class);

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
            LOGGER.error("Failed to get player data for '{}'", targetUsername, exception);
            ChatMessages.GENERIC_ERROR.send(player);
            return;
        }

        if (target == null) {
            ChatMessages.PLAYER_NOT_FOUND.send(player, Component.text(targetUsername));
            return;
        }

        if (!target.online()) {
            ChatMessages.PLAYER_NOT_ONLINE.send(player, Component.text(target.username()));
            return;
        }

        JoinPartyResult result;
        try {
            result = this.partyService.joinParty(player.getUniqueId(), player.getUsername(), target.uuid());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to join '{}' to party of '{}'", target.uuid(), player.getUniqueId(), exception);
            ChatMessages.GENERIC_ERROR.send(player);
            return;
        }

        switch (result) {
            case JoinPartyResult.Success(Party party) -> ChatMessages.YOU_JOINED_PARTY.send(player, Component.text(target.username()));
            case JoinPartyResult.Error error -> {
                switch (error) {
                    case ALREADY_IN_PARTY -> ChatMessages.ERROR_YOU_IN_THIS_PARTY.send(player);
                    case NOT_INVITED -> ChatMessages.ERROR_YOU_NOT_INVITED_TO_PARTY.send(player);
                }
            }
        }
    }
}
