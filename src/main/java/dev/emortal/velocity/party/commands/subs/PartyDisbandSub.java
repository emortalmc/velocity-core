package dev.emortal.velocity.party.commands.subs;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.service.party.ModifyPartyResult;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.velocity.party.commands.PartyCommand;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PartyDisbandSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyListSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();


    private static final Component DISBANDED_MESSAGE = MINI_MESSAGE.deserialize("<green>Party disbanded</green>");
    private static final Component NOT_LEADER_MESSAGE = MINI_MESSAGE.deserialize("""
        <red>You are not the leader of the party
        <red>Use <underlined><click:run_command:'/party leave'>/party leave</click></underlined> to leave the party instead"""
    );


    private final @NotNull PartyService partyService;

    public PartyDisbandSub(@NotNull PartyService partyService) {
        this.partyService = partyService;
    }

    public void execute(@NotNull CommandContext<CommandSource> context) {
        Player executor = (Player) context.getSource();

        ModifyPartyResult result;
        try {
            result = this.partyService.emptyPartyByPlayer(executor.getUniqueId());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to disband party", exception);
            executor.sendMessage(PartyCommand.ERROR_MESSAGE);
            return;
        }

        var message = switch (result) {
            case SUCCESS -> DISBANDED_MESSAGE;
            case NOT_LEADER -> NOT_LEADER_MESSAGE;
        };
        executor.sendMessage(message);
    }
}
