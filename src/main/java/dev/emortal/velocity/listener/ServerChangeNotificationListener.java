package dev.emortal.velocity.listener;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import dev.emortal.api.kurushimi.Assignment;
import dev.emortal.api.kurushimi.Match;
import dev.emortal.api.kurushimi.Ticket;
import dev.emortal.api.kurushimi.messages.MatchCreatedMessage;
import dev.emortal.velocity.messaging.MessagingCore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.UUID;

public class ServerChangeNotificationListener {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String TELEPORT_MESSAGE = "<green>Sending you to <gold><server_id><green>...</green>";

    public ServerChangeNotificationListener(@NotNull ProxyServer proxy, @NotNull MessagingCore messaging) {
        messaging.addListener(MatchCreatedMessage.class, message -> {
            Match match = message.getMatch();
            if (!match.hasAssignment()) return;

            for (Ticket ticket : match.getTicketsList()) {
                if (!ticket.getAutoTeleport()) continue;

                for (String playerId : ticket.getPlayerIdsList()) {
                    proxy.getPlayer(UUID.fromString(playerId)).ifPresent(player -> {
                        Assignment assignment = match.getAssignment();
                        RegisteredServer server = proxy.getServer(assignment.getServerId()).orElse(null);
                        if (server == null) {
                            InetSocketAddress address = InetSocketAddress.createUnresolved(assignment.getServerAddress(), assignment.getServerPort());
                            server = proxy.registerServer(new ServerInfo(assignment.getServerId(), address));
                        }

                        player.sendMessage(MINI_MESSAGE.deserialize(TELEPORT_MESSAGE, Placeholder.unparsed("server_id", assignment.getServerId())));
                        player.createConnectionRequest(server).fireAndForget();
                    });
                }
            }
        });
    }
}
