package dev.emortal.velocity.general.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.service.matchmaker.MatchmakerService;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.velocity.utils.CommandUtils;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LobbyCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(LobbyCommand.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final Component SENDING_MESSAGE = MINI_MESSAGE.deserialize("<green>Sending you to the lobby...");
    private static final Component ERROR_MESSAGE = MINI_MESSAGE.deserialize("<red>Something went wrong while sending you to the lobby!");

    private final MatchmakerService matchmakerService = GrpcStubCollection.getMatchmakerService().orElse(null);

    public LobbyCommand(@NotNull ProxyServer proxy) {
        if (this.matchmakerService == null) {
            LOGGER.error("Matchmaker service unavailable. Lobby command will not be registered.");
            return;
        }

        proxy.getCommandManager().register(this.createBrigadierCommand());
    }

    private @NotNull BrigadierCommand createBrigadierCommand() {
        return new BrigadierCommand(
                LiteralArgumentBuilder.<CommandSource>literal("lobby")
                        .executes(CommandUtils.executeAsync(this::execute))
                        .requires(CommandUtils.isPlayer())
        );
    }

    private void execute(@NotNull CommandContext<CommandSource> context) {
        Player sender = (Player) context.getSource();

        try {
            this.matchmakerService.sendPlayerToLobby(sender.getUniqueId(), false);
            sender.sendMessage(SENDING_MESSAGE);
        } catch (StatusRuntimeException exception) {
            sender.sendMessage(ERROR_MESSAGE);
            LOGGER.error("Error while sending player to lobby (username: {})", sender.getUsername(), exception);
        }
    }
}
