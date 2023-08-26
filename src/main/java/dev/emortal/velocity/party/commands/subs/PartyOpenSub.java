package dev.emortal.velocity.party.commands.subs;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.service.party.ModifyPartyResult;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.velocity.command.ArgumentProvider;
import dev.emortal.velocity.command.EmortalCommandExecutor;
import dev.emortal.velocity.lang.ChatMessages;
import io.grpc.StatusRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PartyOpenSub implements EmortalCommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyOpenSub.class);

    private final @NotNull PartyService partyService;

    public PartyOpenSub(@NotNull PartyService partyService) {
        this.partyService = partyService;
    }

    @Override
    public void execute(@NotNull CommandSource source, @NotNull ArgumentProvider arguments) {
        Player executor = (Player) source;

        ModifyPartyResult result;
        try {
            result = this.partyService.setPartyOpen(executor.getUniqueId(), true);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to open party of '{}'", executor.getUniqueId(), exception);
            ChatMessages.GENERIC_ERROR.send(executor);
            return;
        }

        switch (result) {
            case SUCCESS -> ChatMessages.YOU_OPENED_PARTY.send(executor);
            case NOT_LEADER -> ChatMessages.ERROR_PARTY_NO_PERMISSION.send(executor);
        }
    }
}
