package dev.emortal.velocity.party.commands.subs;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.command.CommandExecutor;
import dev.emortal.api.service.party.ModifyPartyResult;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.velocity.party.commands.PartyCommand;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PartyCloseSub implements CommandExecutor<CommandSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyOpenSub.class);

    private static final Component PARTY_CLOSED_MESSAGE = Component.text("The party is now closed", NamedTextColor.GREEN);
    private static final Component NOT_LEADER_MESSAGE = Component.text("You are not the leader of the party", NamedTextColor.RED);

    private final @NotNull PartyService partyService;

    public PartyCloseSub(@NotNull PartyService partyService) {
        this.partyService = partyService;
    }

    @Override
    public void execute(@NotNull CommandContext<CommandSource> context) {
        Player executor = (Player) context.getSource();

        ModifyPartyResult result;
        try {
            result = this.partyService.setPartyOpen(executor.getUniqueId(), false);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to close party", exception);
            executor.sendMessage(PartyCommand.ERROR_MESSAGE);
            return;
        }

        var message = switch (result) {
            case SUCCESS -> PARTY_CLOSED_MESSAGE;
            case NOT_LEADER -> NOT_LEADER_MESSAGE;
        };
        executor.sendMessage(message);
    }
}
