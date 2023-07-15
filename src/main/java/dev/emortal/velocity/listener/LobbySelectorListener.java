package dev.emortal.velocity.listener;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import dev.emortal.api.kurushimi.messages.MatchCreatedMessage;
import dev.emortal.api.model.matchmaker.Assignment;
import dev.emortal.api.model.matchmaker.Match;
import dev.emortal.api.model.matchmaker.Ticket;
import dev.emortal.api.service.matchmaker.MatchmakerService;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.velocity.messaging.MessagingCore;
import dev.emortal.velocity.utils.Pair;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// TODO let's have some kind of timeout that we can handle with the pendingPlayers
// Maybe we can have a caffeine cache and then resume the Continuation with exception.
public class LobbySelectorListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(LobbySelectorListener.class);

    private static final Component ERROR_MESSAGE = MiniMessage.miniMessage().deserialize("<red>Failed to connect to lobby");

    // NOTE: This is not cleaned up if there's a failed request, we may have problems :skull:
    private final Map<UUID, Pair<PlayerChooseInitialServerEvent, Continuation>> pendingPlayers = new ConcurrentHashMap<>();

    private final MatchmakerService matchmaker = GrpcStubCollection.getMatchmakerService().orElse(null);
    private final ProxyServer proxy;

    public LobbySelectorListener(ProxyServer proxy, MessagingCore messaging) {
        this.proxy = proxy;

        messaging.addListener(MatchCreatedMessage.class, this::handleMatchCreated);
    }

    @Subscribe(async = true)
    public void onInitialServerChoose(PlayerChooseInitialServerEvent event, Continuation continuation) {
        this.sendToLobbyServer(event, continuation);
    }

    private void sendToLobbyServer(PlayerChooseInitialServerEvent event, Continuation continuation) {
        Player player = event.getPlayer();
        this.pendingPlayers.put(player.getUniqueId(), new Pair<>(event, continuation));

        try {
            this.matchmaker.queueInitialLobby(player.getUniqueId());
        } catch (StatusRuntimeException exception) {
            event.getPlayer().disconnect(ERROR_MESSAGE);
            LOGGER.error("Failed to connect player to lobby", exception);
        }
    }

    private void handleMatchCreated(@NotNull MatchCreatedMessage message) {
        Match match = message.getMatch();
        for (Ticket ticket : match.getTicketsList()) {
            if (ticket.getAutoTeleport()) continue; // We don't care about auto teleport tickets
            if (ticket.getPlayerIdsList().size() != 1) continue;

            UUID playerId = UUID.fromString(ticket.getPlayerIds(0));
            Pair<PlayerChooseInitialServerEvent, Continuation> pair = this.pendingPlayers.remove(playerId);

            if (pair == null) continue; // Likely submitted by a different service.

            this.connectPlayerToAssignment(pair.left(), match.getAssignment());
            pair.right().resume();
        }
    }

    private void connectPlayerToAssignment(PlayerChooseInitialServerEvent event, Assignment assignment) {
        RegisteredServer registeredServer = this.proxy.getServer(assignment.getServerId()).orElseGet(() -> {
            InetSocketAddress address = new InetSocketAddress(assignment.getServerAddress(), assignment.getServerPort());
            return this.proxy.registerServer(new ServerInfo(assignment.getServerId(), address));
        });
        event.setInitialServer(registeredServer);
    }
}
