package dev.emortal.velocity.party.commands.subs;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.service.party.JoinPartyResult;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.velocity.command.ArgumentProvider;
import dev.emortal.velocity.command.EmortalCommandExecutor;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.player.resolver.CachedMcPlayer;
import dev.emortal.velocity.player.resolver.PlayerResolver;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PartyJoinSub implements EmortalCommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyJoinSub.class);

    private final @NotNull PartyService partyService;
    private final @NotNull PlayerResolver playerResolver;

    public PartyJoinSub(@NotNull PartyService partyService, @NotNull PlayerResolver playerResolver) {
        this.partyService = partyService;
        this.playerResolver = playerResolver;
    }

    @Override
    public void execute(@NotNull CommandSource source, @NotNull ArgumentProvider arguments) {
        Player player = (Player) source;
        String targetUsername = arguments.getArgument("player", String.class);

        CachedMcPlayer target;
        try {
            target = this.playerResolver.getPlayer(targetUsername);
        } catch (StatusException exception) {
            LOGGER.error("Failed to get player data for '{}'", targetUsername, exception);
            ChatMessages.GENERIC_ERROR.send(player);
            return;
        }

        if (target == null) {
            ChatMessages.PLAYER_NOT_FOUND.send(player, targetUsername);
            return;
        }

        if (!target.online()) {
            ChatMessages.PLAYER_NOT_ONLINE.send(player, target.username());
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
            case JoinPartyResult.Success ignored -> ChatMessages.YOU_JOINED_PARTY.send(player, target.username());
            case JoinPartyResult.Error error -> {
                switch (error) {
                    case ALREADY_IN_PARTY -> ChatMessages.ERROR_ALREADY_IN_PARTY.send(player);
                    case NOT_INVITED -> ChatMessages.ERROR_YOU_NOT_INVITED_TO_PARTY.send(player);
                }
            }
        }
    }
}
