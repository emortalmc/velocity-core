package dev.emortal.velocity.matchmaking.listener;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.emortal.api.message.matchmaker.MatchCreatedMessage;
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
import java.util.function.Consumer;

public final class LobbySelectorListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(LobbySelectorListener.class);

    private final @NotNull ServerProvider serverProvider;
    private final @NotNull MatchmakerService matchmaker;

    // NOTE: This is not cleaned up if there's a failed request, we may have problems :skull:
    private final Cache<UUID, Consumer<@Nullable RegisteredServer>> pendingPlayers = Caffeine.newBuilder()
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

    @Subscribe
    public void handleKickedFromServer(@NotNull KickedFromServerEvent event, @NotNull Continuation continuation) {
        if (event.kickedDuringServerConnect()) return;

        this.pendingPlayers.put(event.getPlayer().getUniqueId(), server -> {
            if (server == null) {
                LOGGER.error("Failed to find fallback lobby match for '{}'", event.getPlayer().getUsername());
                event.getPlayer().disconnect(ChatMessages.ERROR_CONNECTING_TO_FALLBACK_LOBBY.get());
                return;
            }

            event.setResult(KickedFromServerEvent.RedirectPlayer.create(server));
            continuation.resume();
        });

        this.matchmaker.loginQueue(event.getPlayer().getUniqueId(), false);
    }

    private void sendToLobbyServer(@NotNull PlayerChooseInitialServerEvent event, @NotNull Continuation continuation) {
        Player player = event.getPlayer();
        LOGGER.debug("Queueing initial lobby for '{}'", player.getUsername());

        try {
            this.matchmaker.loginQueue(player.getUniqueId(), false);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to connect '{}' to lobby", player.getUsername(), exception);
            event.getPlayer().disconnect(ChatMessages.ERROR_CONNECTING_TO_LOBBY.get());
        }

        this.pendingPlayers.put(player.getUniqueId(), server -> {
            if (server == null) {
                LOGGER.error("Failed to find initial lobby match for '{}'", player.getUsername());
                player.disconnect(ChatMessages.ERROR_CONNECTING_TO_LOBBY.get());
                return;
            }

            event.setInitialServer(server);
            continuation.resume();
        });
    }

    private void handleMatchCreated(@NotNull MatchCreatedMessage message) {
        Match match = message.getMatch();
        if (!match.getGameModeId().equals("lobby")) return;

        for (Ticket ticket : match.getTicketsList()) {
            if (ticket.getAutoTeleport()) continue; // We don't care about auto teleport tickets
            if (ticket.getPlayerIdsList().size() != 1) continue;

            UUID playerId = UUID.fromString(ticket.getPlayerIds(0));

            Consumer<RegisteredServer> consumer = this.pendingPlayers.getIfPresent(playerId);
            if (consumer == null) continue; // Likely submitted by a different service.

            this.pendingPlayers.invalidate(playerId);
            consumer.accept(this.serverProvider.createServerFromAssignment(match.getAssignment()));
        }
    }

    private void onEvict(@Nullable UUID playerId, @Nullable Consumer<@Nullable RegisteredServer> consumer, @NotNull RemovalCause cause) {
        if (cause != RemovalCause.EXPIRED) return;
        if (playerId == null || consumer == null) return;

        consumer.accept(null);
    }
}
