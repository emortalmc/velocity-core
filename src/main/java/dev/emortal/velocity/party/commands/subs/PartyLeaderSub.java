package dev.emortal.velocity.party.commands.subs;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.api.service.party.SetPartyLeaderResult;
import dev.emortal.api.utils.resolvers.PlayerResolver;
import dev.emortal.velocity.lang.TempLang;
import dev.emortal.velocity.party.commands.PartyCommand;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PartyLeaderSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyLeaderSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();


    private static final String UPDATED_LEADER_MESSAGE = "<green>Successfully updated the party leader";
    private static final String NOT_LEADER_MESSAGE = "<red>You must be the party leader to update the party leader";
    private static final String NOT_IN_PARTY_MESSAGE = "<red><username> is not in your party";


    private final @NotNull PartyService partyService;

    public PartyLeaderSub(@NotNull PartyService partyService) {
        this.partyService = partyService;
    }

    public void execute(@NotNull CommandContext<CommandSource> context) {
        String targetUsername = context.getArgument("player", String.class);
        Player executor = (Player) context.getSource();

        PlayerResolver.CachedMcPlayer target;
        try {
            target = PlayerResolver.getPlayerData(targetUsername);
        } catch (StatusException exception) {
            LOGGER.error("An error occurred PartyLeaderSub retrievePlayerData: {}", exception);
            executor.sendMessage(PartyCommand.ERROR_MESSAGE);
            return;
        }

        if (target == null) {
            TempLang.PLAYER_NOT_FOUND.send(executor, Placeholder.unparsed("search_username", targetUsername));
            return;
        }

        SetPartyLeaderResult result;
        try {
            result = this.partyService.setPartyLeader(executor.getUniqueId(), executor.getUsername(), target.uuid());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("An error occurred PartyLeaderSub setPartyLeader: ", exception);
            executor.sendMessage(PartyCommand.ERROR_MESSAGE);
            return;
        }

        var message = switch (result) {
            case SUCCESS -> MINI_MESSAGE.deserialize(UPDATED_LEADER_MESSAGE);
            case SELF_NOT_LEADER -> MINI_MESSAGE.deserialize(NOT_LEADER_MESSAGE);
            case TARGET_NOT_IN_PARTY -> MINI_MESSAGE.deserialize(NOT_IN_PARTY_MESSAGE, Placeholder.unparsed("username", target.username()));
        };
        executor.sendMessage(message);
    }
}
