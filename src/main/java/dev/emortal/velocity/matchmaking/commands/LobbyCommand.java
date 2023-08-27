package dev.emortal.velocity.matchmaking.commands;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.service.matchmaker.MatchmakerService;
import dev.emortal.velocity.command.CommandConditions;
import dev.emortal.velocity.command.EmortalCommand;
import dev.emortal.velocity.lang.ChatMessages;
import io.grpc.StatusRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LobbyCommand extends EmortalCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(LobbyCommand.class);

    private final MatchmakerService matchmaker;

    public LobbyCommand(@NotNull MatchmakerService matchmaker) {
        super("lobby");
        this.matchmaker = matchmaker;

        super.setCondition(CommandConditions.playerOnly());
        super.setDefaultExecutor(this::execute);
    }

    private void execute(@NotNull CommandContext<CommandSource> context) {
        Player sender = (Player) context.getSource();

        try {
            this.matchmaker.sendPlayerToLobby(sender.getUniqueId(), false);
            ChatMessages.SENDING_TO_LOBBY.send(sender);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to send '{}' to lobby", sender.getUsername(), exception);
            ChatMessages.ERROR_SENDING_TO_LOBBY.send(sender);
        }
    }
}
