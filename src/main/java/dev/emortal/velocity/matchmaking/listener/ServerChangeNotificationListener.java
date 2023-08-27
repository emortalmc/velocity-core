package dev.emortal.velocity.matchmaking.listener;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import dev.emortal.api.kurushimi.messages.MatchCreatedMessage;
import dev.emortal.api.model.matchmaker.Assignment;
import dev.emortal.api.model.matchmaker.Match;
import dev.emortal.api.model.matchmaker.Ticket;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.messaging.MessagingModule;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.UUID;

public final class ServerChangeNotificationListener {

    private final ProxyServer proxy;

    public ServerChangeNotificationListener(@NotNull ProxyServer proxy, @NotNull MessagingModule messaging) {
        this.proxy = proxy;
        messaging.addListener(MatchCreatedMessage.class, message -> this.onMatchCreated(message.getMatch()));
    }

    private void onMatchCreated(@NotNull Match match) {
        if (!match.hasAssignment()) return;
        Assignment assignment = match.getAssignment();

        for (Ticket ticket : match.getTicketsList()) {
            if (!ticket.getAutoTeleport()) continue;

            for (String playerId : ticket.getPlayerIdsList()) {
                UUID uuid = UUID.fromString(playerId);

                Player player = this.proxy.getPlayer(uuid).orElse(null);
                if (player == null) continue;

                RegisteredServer server = this.proxy.getServer(assignment.getServerId()).orElse(null);
                if (server == null) {
                    InetSocketAddress address = InetSocketAddress.createUnresolved(assignment.getServerAddress(), assignment.getServerPort());
                    server = this.proxy.registerServer(new ServerInfo(assignment.getServerId(), address));
                }

                ChatMessages.SENDING_TO_SERVER.send(player, Component.text(assignment.getServerId()));
                player.createConnectionRequest(server).fireAndForget();
            }
        }
    }
}
