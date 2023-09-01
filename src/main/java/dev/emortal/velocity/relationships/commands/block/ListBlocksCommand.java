package dev.emortal.velocity.relationships.commands.block;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.model.mcplayer.McPlayer;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.api.service.relationship.RelationshipService;
import dev.emortal.velocity.command.ArgumentProvider;
import dev.emortal.velocity.command.EmortalCommand;
import dev.emortal.velocity.command.EmortalCommandExecutor;
import dev.emortal.velocity.lang.ChatMessages;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public final class ListBlocksCommand extends EmortalCommand implements EmortalCommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListBlocksCommand.class);

    private final @NotNull RelationshipService relationshipService;
    private final @NotNull McPlayerService playerService;

    public ListBlocksCommand(@NotNull RelationshipService relationshipService, @NotNull McPlayerService playerService) {
        super("listblocks");
        this.relationshipService = relationshipService;
        this.playerService = playerService;

        super.setPlayerOnly();
        super.setDefaultExecutor(this);
    }

    @Override
    public void execute(@NotNull CommandSource source, @NotNull ArgumentProvider arguments) {
        Player sender = (Player) source;

        List<UUID> blockedIds;
        try {
            blockedIds = this.relationshipService.getBlockedList(sender.getUniqueId());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to get blocked player IDs for '{}'", sender.getUsername(), exception);
            ChatMessages.GENERIC_ERROR.send(sender);
            return;
        }

        if (blockedIds.isEmpty()) {
            ChatMessages.ERROR_BLOCKED_LIST_EMPTY.send(sender);
            return;
        }

        List<McPlayer> blockedPlayers;
        try {
            blockedPlayers = this.playerService.getPlayersById(blockedIds);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to resolve blocked players from IDs '{}'", blockedIds, exception);
            ChatMessages.GENERIC_ERROR.send(sender);
            return;
        }

        String blockedNames = String.join(", ", blockedPlayers.stream()
                .map(McPlayer::getCurrentUsername)
                .toList());

        ChatMessages.BLOCKED_PLAYERS.send(sender, Component.text(blockedPlayers.size()), Component.text(blockedNames));
    }
}
