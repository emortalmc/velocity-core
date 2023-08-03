package dev.emortal.velocity.relationships.commands.block;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.model.mcplayer.McPlayer;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.api.service.relationship.RelationshipService;
import dev.emortal.velocity.command.CommandConditions;
import dev.emortal.velocity.command.EmortalCommand;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public final class ListBlocksCommand extends EmortalCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListBlocksCommand.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String BLOCKED_PLAYERS_MESSAGE = "<red>Blocked Players (<count>): <blocked_players></red>";

    private final McPlayerService mcPlayerService;
    private final RelationshipService relationshipService;

    public ListBlocksCommand(@NotNull McPlayerService mcPlayerService, @NotNull RelationshipService relationshipService) {
        super("listblocks");
        this.mcPlayerService = mcPlayerService;
        this.relationshipService = relationshipService;

        super.setCondition(CommandConditions.playerOnly());
        super.setDefaultExecutor(this::execute);
    }

    private void execute(@NotNull CommandContext<CommandSource> context) {
        Player sender = (Player) context.getSource();

        List<UUID> blockedIds;
        try {
            blockedIds = this.relationshipService.getBlockedList(sender.getUniqueId());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("An error occurred while trying to list your blocked players", exception);
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>An error occurred while trying to list your blocked players"));
            return;
        }

        if (blockedIds.isEmpty()) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>You have not blocked any players"));
            return;
        }

        List<McPlayer> blockedPlayers;
        try {
            blockedPlayers = this.mcPlayerService.getPlayersById(blockedIds);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("An error occurred while trying to list your blocked players", exception);
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>An error occurred while trying to list your blocked players"));
            return;
        }

        String blockedNames = String.join(", ", blockedPlayers.stream()
                .map(McPlayer::getCurrentUsername)
                .toList());

        Component message = MINI_MESSAGE.deserialize(BLOCKED_PLAYERS_MESSAGE,
                Placeholder.unparsed("count", String.valueOf(blockedPlayers.size())),
                Placeholder.unparsed("blocked_players", blockedNames));

        sender.sendMessage(message);
    }
}
