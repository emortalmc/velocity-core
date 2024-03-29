package dev.emortal.velocity.party.commands.party.subs;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.service.party.InvitePlayerToPartyResult;
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

public final class PartyInviteSub implements EmortalCommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyInviteSub.class);

    private final @NotNull PartyService partyService;
    private final @NotNull PlayerResolver playerResolver;

    public PartyInviteSub(@NotNull PartyService partyService, @NotNull PlayerResolver playerResolver) {
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

        InvitePlayerToPartyResult result;
        try {
            result = this.partyService.invitePlayer(player.getUniqueId(), player.getUsername(), target.uuid(), target.username());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to invite '{}' to party of '{}'", target.uuid(), player.getUniqueId(), exception);
            ChatMessages.GENERIC_ERROR.send(player);
            return;
        }

        switch (result) {
            case InvitePlayerToPartyResult.Success ignored -> ChatMessages.YOU_INVITED_PLAYER_TO_PARTY.send(player, target.username());
            case InvitePlayerToPartyResult.Error error -> {
                switch (error) {
                    case NO_PERMISSION -> ChatMessages.ERROR_PARTY_NO_PERMISSION.send(player);
                    case TARGET_ALREADY_INVITED -> ChatMessages.ERROR_PLAYER_INVITED_TO_PARTY.send(player, target.username());
                    case TARGET_IN_THIS_PARTY -> ChatMessages.ERROR_PLAYER_IN_THIS_PARTY.send(player, target.username());
                }
            }
        }
    }
}
