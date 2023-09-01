package dev.emortal.velocity.matchmaking.listener;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.emortal.api.message.matchmaker.MatchCreatedMessage;
import dev.emortal.api.model.matchmaker.Assignment;
import dev.emortal.api.model.matchmaker.Match;
import dev.emortal.api.model.matchmaker.Ticket;
import dev.emortal.velocity.adapter.server.ServerProvider;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.messaging.MessagingModule;
import dev.emortal.velocity.adapter.player.PlayerProvider;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class ServerChangeNotificationListener {

    private final @NotNull PlayerProvider playerProvider;
    private final @NotNull ServerProvider serverProvider;

    public ServerChangeNotificationListener(@NotNull PlayerProvider playerProvider, @NotNull ServerProvider serverProvider,
                                            @NotNull MessagingModule messaging) {
        this.playerProvider = playerProvider;
        this.serverProvider = serverProvider;

        messaging.addListener(MatchCreatedMessage.class, message -> this.onMatchCreated(message.getMatch()));
    }

    private void onMatchCreated(@NotNull Match match) {
        if (!match.hasAssignment()) return;
        Assignment assignment = match.getAssignment();

        for (Ticket ticket : match.getTicketsList()) {
            if (!ticket.getAutoTeleport()) continue;

            for (String playerId : ticket.getPlayerIdsList()) {
                UUID uuid = UUID.fromString(playerId);

                Player player = this.playerProvider.getPlayer(uuid);
                if (player == null) continue;

                RegisteredServer server = this.serverProvider.getServer(assignment.getServerId());
                if (server == null) {
                    server = this.serverProvider.createServer(assignment.getServerId(), assignment.getServerAddress(), assignment.getServerPort());
                }

                ChatMessages.SENDING_TO_SERVER.send(player, Component.text(assignment.getServerId()));
                player.createConnectionRequest(server).fireAndForget();
            }
        }
    }
}