package dev.emortal.velocity.party.commands.subs;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.command.CommandExecutor;
import dev.emortal.api.service.party.LeavePartyResult;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.velocity.lang.ChatMessages;
import io.grpc.StatusRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PartyLeaveSub implements CommandExecutor<CommandSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyLeaveSub.class);

    private final @NotNull PartyService partyService;

    public PartyLeaveSub(@NotNull PartyService partyService) {
        this.partyService = partyService;
    }

    @Override
    public void execute(@NotNull CommandContext<CommandSource> context) {
        // context is ignored
        Player executor = (Player) context.getSource();

        LeavePartyResult result;
        try {
            result = this.partyService.leaveParty(executor.getUniqueId());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to remove '{}' from party", executor.getUniqueId(), exception);
            ChatMessages.GENERIC_ERROR.send(executor);
            return;
        }

        switch (result) {
            case SUCCESS -> ChatMessages.YOU_LEFT_PARTY.send(executor);
            case CANNOT_LEAVE_AS_LEADER -> ChatMessages.ERROR_CANNOT_LEAVE_AS_LEADER.send(executor);
        }
    }
}
