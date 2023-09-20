package dev.emortal.velocity.matchmaking.listener;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.emortal.api.message.matchmaker.MatchCreatedMessage;
import dev.emortal.api.model.matchmaker.Assignment;
import dev.emortal.api.model.matchmaker.Match;
import dev.emortal.api.model.matchmaker.Ticket;
import dev.emortal.api.service.matchmaker.MatchmakerService;
import dev.emortal.velocity.adapter.server.ServerProvider;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.messaging.MessagingModule;
import io.grpc.StatusRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class LobbySelectorListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(LobbySelectorListener.class);

    private final @NotNull ServerProvider serverProvider;
    private final @NotNull MatchmakerService matchmaker;

    // NOTE: This is not cleaned up if there's a failed request, we may have problems :skull:
    private final Cache<UUID, EventCallbackContext> pendingPlayers = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .evictionListener(this::onEvict)
            .build();

    public LobbySelectorListener(@NotNull ServerProvider serverProvider, @NotNull MatchmakerService matchmaker, @NotNull MessagingModule messaging) {
        this.serverProvider = serverProvider;
        this.matchmaker = matchmaker;

        messaging.addListener(MatchCreatedMessage.class, this::handleMatchCreated);
    }

    @Subscribe
    public void onInitialServerChoose(@NotNull PlayerChooseInitialServerEvent event, @NotNull Continuation continuation) {
        this.sendToLobbyServer(event, continuation);
    }

    private void sendToLobbyServer(@NotNull PlayerChooseInitialServerEvent event, @NotNull Continuation continuation) {
        Player player = event.getPlayer();
        LOGGER.debug("Queueing initial lobby for '{}'", player.getUsername());

        try {
            this.matchmaker.queueInitialLobby(player.getUniqueId());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to connect '{}' to lobby", player.getUsername(), exception);
            event.getPlayer().disconnect(ChatMessages.ERROR_CONNECTING_TO_LOBBY.parse());
        }

        this.pendingPlayers.put(player.getUniqueId(), new EventCallbackContext(event, continuation));
    }

    private void handleMatchCreated(@NotNull MatchCreatedMessage message) {
        Match match = message.getMatch();

        for (Ticket ticket : match.getTicketsList()) {
            if (ticket.getAutoTeleport()) continue; // We don't care about auto teleport tickets
            if (ticket.getPlayerIdsList().size() != 1) continue;

            UUID playerId = UUID.fromString(ticket.getPlayerIds(0));

            EventCallbackContext context = this.pendingPlayers.getIfPresent(playerId);
            if (context == null) continue; // Likely submitted by a different service.

            LOGGER.debug("Found initial lobby match for '{}': {}", context.playerName(), match);
            this.pendingPlayers.invalidate(playerId);
            this.connectPlayerToAssignment(context, match.getAssignment());
        }
    }

    private void connectPlayerToAssignment(@NotNull EventCallbackContext context, @NotNull Assignment assignment) {
        LOGGER.debug("Connecting '{}' to {}", context.playerName(), assignment);

        RegisteredServer server = this.serverProvider.createServerFromAssignment(assignment);
        context.setInitialServer(server);
    }

    private void onEvict(@Nullable UUID playerId, @Nullable EventCallbackContext context, @NotNull RemovalCause cause) {
        if (cause != RemovalCause.EXPIRED) return;
        if (playerId == null || context == null) return;

        LOGGER.debug("Failed to find initial lobby match in time for '{}'", context.playerName());
        context.disconnect();
    }

    private record EventCallbackContext(@NotNull PlayerChooseInitialServerEvent event, @NotNull Continuation continuation) {

        @NotNull String playerName() {
            return this.event.getPlayer().getUsername();
        }

        void setInitialServer(@NotNull RegisteredServer server) {
            this.event.setInitialServer(server);
            this.continuation.resume();
        }

        void disconnect() {
            this.event.getPlayer().disconnect(ChatMessages.ERROR_CONNECTING_TO_LOBBY.parse());
            this.continuation.resume();
        }
    }
}
