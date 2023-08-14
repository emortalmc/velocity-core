package dev.emortal.velocity.matchmaking.commands;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.service.matchmaker.MatchmakerService;
import dev.emortal.velocity.command.CommandConditions;
import dev.emortal.velocity.command.EmortalCommand;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LobbyCommand extends EmortalCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(LobbyCommand.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final Component SENDING_MESSAGE = MINI_MESSAGE.deserialize("<green>Sending you to the lobby...");
    private static final Component ERROR_MESSAGE = MINI_MESSAGE.deserialize("<red>Something went wrong while sending you to the lobby!");

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
            sender.sendMessage(SENDING_MESSAGE);
        } catch (StatusRuntimeException exception) {
            sender.sendMessage(ERROR_MESSAGE);
            LOGGER.error("Error while sending player to lobby (username: {})", sender.getUsername(), exception);
        }
    }
}
