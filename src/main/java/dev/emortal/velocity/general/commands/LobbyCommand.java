package dev.emortal.velocity.general.commands;

import com.google.common.util.concurrent.Futures;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.kurushimi.KurushimiStubCollection;
import dev.emortal.api.kurushimi.MatchmakerGrpc;
import dev.emortal.api.kurushimi.SendPlayerToLobbyRequest;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import dev.emortal.velocity.utils.CommandUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;

public class LobbyCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(LobbyCommand.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final Component SENDING_MESSAGE = MINI_MESSAGE.deserialize("<green>Sending you to the lobby...");
    private static final Component ERROR_MESSAGE = MINI_MESSAGE.deserialize("<red>Something went wrong while sending you to the lobby!");

    private final MatchmakerGrpc.MatchmakerFutureStub matchmakerService = KurushimiStubCollection.getFutureStub().orElse(null);

    public LobbyCommand(@NotNull ProxyServer proxy) {
        if (this.matchmakerService == null) return;

        proxy.getCommandManager().register(this.createBrigadierCommand());
    }

    private @NotNull BrigadierCommand createBrigadierCommand() {
        return new BrigadierCommand(
                LiteralArgumentBuilder.<CommandSource>literal("lobby")
                        .executes(this::execute)
                        .requires(CommandUtils.isPlayer())
        );
    }

    private int execute(@NotNull CommandContext<CommandSource> context) {
        Player sender = (Player) context.getSource();

        var lobbyReqFuture = this.matchmakerService.sendPlayersToLobby(SendPlayerToLobbyRequest.newBuilder()
                .addPlayerIds(sender.getUniqueId().toString())
                .setSendParties(false)
                .build());

        Futures.addCallback(lobbyReqFuture, FunctionalFutureCallback.create(
                ignored -> sender.sendMessage(SENDING_MESSAGE),
                throwable -> {
                    sender.sendMessage(ERROR_MESSAGE);
                    LOGGER.error("Error while sending player to lobby (username: {}): {}", sender.getUsername(), throwable);
                }
        ), ForkJoinPool.commonPool());

        return 1;
    }
}
