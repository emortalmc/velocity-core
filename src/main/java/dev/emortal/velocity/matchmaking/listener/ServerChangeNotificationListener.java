package dev.emortal.velocity.matchmaking.listener;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.platform.ViaServerProxyPlatform;
import dev.emortal.api.message.gamesdk.GameReadyMessage;
import dev.emortal.api.message.matchmaker.MatchCreatedMessage;
import dev.emortal.api.model.matchmaker.Assignment;
import dev.emortal.api.model.matchmaker.Match;
import dev.emortal.api.model.matchmaker.Ticket;
import dev.emortal.velocity.adapter.player.PlayerProvider;
import dev.emortal.velocity.adapter.server.ServerProvider;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.messaging.MessagingModule;
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
        messaging.addListener(GameReadyMessage.class, message -> this.onGameReady(message.getMatch()));
    }

    private void onMatchCreated(@NotNull Match match) {
        if (!match.getGameModeId().equals("lobby")) return;
        this.movePlayersToMatch(match);
    }

    private void onGameReady(@NotNull Match match) {
        this.movePlayersToMatch(match);
    }

    private void movePlayersToMatch(@NotNull Match match) {
        if (!match.hasAssignment()) return;

        Assignment assignment = match.getAssignment();
        String serverId = assignment.getServerId();

        for (Ticket ticket : match.getTicketsList()) {
            if (!ticket.getAutoTeleport()) continue;

            for (String playerId : ticket.getPlayerIdsList()) {
                UUID uuid = UUID.fromString(playerId);

                Player player = this.playerProvider.getPlayer(uuid);
                if (player == null) continue;

                if (this.isPlayerOnTargetServer(player, serverId)) {
                    // Don't move players already on the target server
                    continue;
                }

                RegisteredServer server = this.serverProvider.createServerFromAssignment(assignment);

                // Retrieve ViaVersion, set the protocol version if available.
                // This allows us to run old server versions for *some* games.
                ViaServerProxyPlatform platform = (ViaServerProxyPlatform) Via.getPlatform();

                if (assignment.hasProtocolVersion()) {
                    platform.protocolDetectorService().setProtocolVersion(assignment.getServerId(), (int) assignment.getProtocolVersion());
                }

                ChatMessages.SENDING_TO_SERVER.send(player, serverId);
                player.createConnectionRequest(server).fireAndForget();
            }
        }
    }

    private boolean isPlayerOnTargetServer(@NotNull Player player, @NotNull String targetServerId) {
        ServerConnection connection = player.getCurrentServer().orElse(null);
        if (connection == null) return false;

        ServerInfo serverInfo = connection.getServerInfo();
        return serverInfo.getName().equals(targetServerId);
    }
}
