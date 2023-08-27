package dev.emortal.velocity.party.commands.subs;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.command.CommandExecutor;
import dev.emortal.api.model.party.PartyInvite;
import dev.emortal.api.service.party.InvitePlayerToPartyResult;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.api.utils.resolvers.PlayerResolver;
import dev.emortal.velocity.lang.ChatMessages;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PartyInviteSub implements CommandExecutor<CommandSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyInviteSub.class);

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

        InvitePlayerToPartyResult result;
        try {
            result = this.partyService.invitePlayer(player.getUniqueId(), player.getUsername(), target.uuid(), target.username());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to invite '{}' to party of '{}'", target.uuid(), player.getUniqueId(), exception);
            ChatMessages.GENERIC_ERROR.send(player);
            return;
        }

        switch (result) {
            case InvitePlayerToPartyResult.Success(PartyInvite ignored) ->
                    ChatMessages.YOU_INVITED_PLAYER_TO_PARTY.send(player, Component.text(target.username()));
            case InvitePlayerToPartyResult.Error error -> {
                switch (error) {
                    case NO_PERMISSION -> ChatMessages.ERROR_PARTY_NO_PERMISSION.send(player);
                    case TARGET_ALREADY_INVITED -> ChatMessages.ERROR_PLAYER_INVITED_TO_PARTY.send(player, Component.text(target.username()));
                    case TARGET_IN_THIS_PARTY -> ChatMessages.ERROR_PLAYER_IN_THIS_PARTY.send(player, Component.text(target.username()));
                }
            }
        }
    }
}
