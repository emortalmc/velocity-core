package dev.emortal.velocity.party.commands.subs;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.service.party.LeavePartyResult;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.velocity.party.commands.PartyCommand;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PartyLeaveSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyLeaveSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();


    private static final Component LEFT_MESSAGE = MINI_MESSAGE.deserialize("<green>Left party");
    private static final Component LEFT_AS_LEADER_MESSAGE = MINI_MESSAGE.deserialize("""
            <red>You are the leader of the party
            <red>Use /party disband to disband the party or /party leader <player> to transfer leadership""");


    private final @NotNull PartyService partyService;

    public PartyLeaveSub(@NotNull PartyService partyService) {
        this.partyService = partyService;
    }

    public void execute(CommandContext<CommandSource> context) {
        // context is ignored
        Player executor = (Player) context.getSource();

        LeavePartyResult result;
        try {
            result = this.partyService.leaveParty(executor.getUniqueId());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to leave party", exception);
            executor.sendMessage(PartyCommand.ERROR_MESSAGE);
            return;
        }

        var message = switch (result) {
            case SUCCESS -> LEFT_MESSAGE;
            case CANNOT_LEAVE_AS_LEADER -> LEFT_AS_LEADER_MESSAGE;
        };
        executor.sendMessage(message);
    }
}
