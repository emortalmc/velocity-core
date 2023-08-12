package dev.emortal.velocity.matchmaking.listener;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import dev.emortal.api.kurushimi.messages.MatchCreatedMessage;
import dev.emortal.api.model.matchmaker.Assignment;
import dev.emortal.api.model.matchmaker.Match;
import dev.emortal.api.model.matchmaker.Ticket;
import dev.emortal.velocity.messaging.MessagingModule;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.UUID;

public final class ServerChangeNotificationListener {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String TELEPORT_MESSAGE = "<green>Sending you to <gold><server_id><green>...</green>";

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

                player.sendMessage(MINI_MESSAGE.deserialize(TELEPORT_MESSAGE, Placeholder.unparsed("server_id", assignment.getServerId())));
                player.createConnectionRequest(server).fireAndForget();
            }
        }
    }
}
