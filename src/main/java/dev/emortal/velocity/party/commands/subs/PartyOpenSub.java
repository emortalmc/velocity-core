package dev.emortal.velocity.party.commands.subs;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.service.party.ModifyPartyResult;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.velocity.party.PartyCache;
import dev.emortal.velocity.party.commands.PartyCommand;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PartyOpenSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyOpenSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();


    private static final Component PARTY_CLOSED_MESSAGE = MINI_MESSAGE.deserialize("<green>The party is now closed");
    private static final Component PARTY_OPENED_MESSAGE = MINI_MESSAGE.deserialize("<green>The party is now open");
    private static final Component NOT_LEADER_MESSAGE = MINI_MESSAGE.deserialize("<red>You are not the leader of the party");


    private final @NotNull PartyService partyService;
    private final @NotNull PartyCache partyCache;

    public PartyOpenSub(@NotNull PartyService partyService, @NotNull PartyCache partyCache) {
        this.partyService = partyService;
        this.partyCache = partyCache;
    }

    public void execute(@NotNull CommandContext<CommandSource> context) {
        Player executor = (Player) context.getSource();
        PartyCache.CachedParty party = this.partyCache.getPlayerParty(executor.getUniqueId());
        if (party == null) {
            executor.sendMessage(PartyCommand.NOT_IN_PARTY_MESSAGE);
            return;
        }

        ModifyPartyResult result;
        try {
            result = this.partyService.setPartyOpen(executor.getUniqueId(), !party.isOpen());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to open party", exception);
            executor.sendMessage(PartyCommand.ERROR_MESSAGE);
            return;
        }

        var message = switch (result) {
            case SUCCESS -> {
                party.setOpen(!party.isOpen());
                yield party.isOpen() ? PARTY_OPENED_MESSAGE : PARTY_CLOSED_MESSAGE;
            }
            case NOT_LEADER -> NOT_LEADER_MESSAGE;
        };
        executor.sendMessage(message);
    }
}
