package dev.emortal.velocity.party.commands.subs;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.command.CommandExecutor;
import dev.emortal.api.service.party.ModifyPartyResult;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.velocity.lang.ChatMessages;
import io.grpc.StatusRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PartyDisbandSub implements CommandExecutor<CommandSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyListSub.class);

    private final @NotNull PartyService partyService;

    public PartyDisbandSub(@NotNull PartyService partyService) {
        this.partyService = partyService;
    }

    @Override
    public void execute(@NotNull CommandContext<CommandSource> context) {
        Player executor = (Player) context.getSource();

        ModifyPartyResult result;
        try {
            result = this.partyService.emptyPartyByPlayer(executor.getUniqueId());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to disband party of '{}'", executor.getUsername(), exception);
            ChatMessages.GENERIC_ERROR.send(executor);
            return;
        }

        switch (result) {
            case SUCCESS -> ChatMessages.YOU_DISBANDED_PARTY.send(executor);
            case NOT_LEADER -> ChatMessages.ERROR_NOT_PARTY_LEADER_DISBAND.send(executor);
        }
    }
}
