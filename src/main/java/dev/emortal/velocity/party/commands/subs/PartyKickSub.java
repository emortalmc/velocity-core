package dev.emortal.velocity.party.commands.subs;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.command.CommandExecutor;
import dev.emortal.api.service.party.KickPlayerFromPartyResult;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.player.resolver.CachedMcPlayer;
import dev.emortal.velocity.player.resolver.PlayerResolver;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PartyKickSub implements CommandExecutor<CommandSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyKickSub.class);

    private final @NotNull PartyService partyService;
    private final @NotNull PlayerResolver playerResolver;

    public PartyKickSub(@NotNull PartyService partyService, @NotNull PlayerResolver playerResolver) {
        this.partyService = partyService;
        this.playerResolver = playerResolver;
    }

    @Override
    public void execute(@NotNull CommandContext<CommandSource> context) {
        Player executor = (Player) context.getSource();
        String targetUsername = context.getArgument("player", String.class);

        CachedMcPlayer target;
        try {
            target = this.playerResolver.getPlayer(targetUsername);
        } catch (StatusException exception) {
            LOGGER.error("Failed to get player data for '{}'", targetUsername, exception);
            ChatMessages.GENERIC_ERROR.send(executor);
            return;
        }

        if (target == null) {
            ChatMessages.PLAYER_NOT_FOUND.send(executor, Component.text(targetUsername));
            return;
        }

        KickPlayerFromPartyResult result;
        try {
            result = this.partyService.kickPlayer(executor.getUniqueId(), executor.getUsername(), target.uuid());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to kick '{}' from party of '{}'", target.uuid(), executor.getUniqueId(), exception);
            ChatMessages.GENERIC_ERROR.send(executor);
            return;
        }

        switch (result) {
            case SUCCESS -> ChatMessages.YOU_KICKED_PLAYER_FROM_PARTY.send(executor, Component.text(target.username()));
            case SELF_NOT_LEADER -> ChatMessages.ERROR_PARTY_NO_PERMISSION.send(executor);
            case TARGET_IS_LEADER -> ChatMessages.ERROR_CANNOT_KICK_LEADER.send(executor);
            case TARGET_NOT_IN_PARTY -> ChatMessages.ERROR_PLAYER_NOT_IN_PARTY.send(executor, Component.text(target.username()));
        }
    }
}
