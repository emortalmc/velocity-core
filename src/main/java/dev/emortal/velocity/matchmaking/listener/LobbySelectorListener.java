package dev.emortal.velocity.matchmaking.listener;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
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
import dev.emortal.velocity.messaging.MessagingModule;
import dev.emortal.velocity.utils.Pair;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class LobbySelectorListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(LobbySelectorListener.class);

    private static final Component ERROR_MESSAGE = MiniMessage.miniMessage().deserialize("<red>Failed to connect to lobby");

    private final ProxyServer proxy;
    private final MatchmakerService matchmaker;

    // NOTE: This is not cleaned up if there's a failed request, we may have problems :skull:
    private final Cache<UUID, Pair<PlayerChooseInitialServerEvent, Continuation>> pendingPlayers = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .evictionListener(this::onEvict)
            .build();

    public LobbySelectorListener(@NotNull ProxyServer proxy, @NotNull MatchmakerService matchmaker, @NotNull MessagingModule messaging) {
        this.proxy = proxy;
        this.matchmaker = matchmaker;

        messaging.addListener(MatchCreatedMessage.class, this::handleMatchCreated);
    }

    @Subscribe
    public void onInitialServerChoose(@NotNull PlayerChooseInitialServerEvent event, @NotNull Continuation continuation) {
        this.sendToLobbyServer(event, continuation);
    }

    private void sendToLobbyServer(@NotNull PlayerChooseInitialServerEvent event, @NotNull Continuation continuation) {
        Player player = event.getPlayer();
        try {
            this.matchmaker.queueInitialLobby(player.getUniqueId());
        } catch (StatusRuntimeException exception) {
            event.getPlayer().disconnect(ERROR_MESSAGE);
            LOGGER.error("Failed to connect player to lobby", exception);
        }

        this.pendingPlayers.put(player.getUniqueId(), new Pair<>(event, continuation));
    }

    private void handleMatchCreated(@NotNull MatchCreatedMessage message) {
        Match match = message.getMatch();

        for (Ticket ticket : match.getTicketsList()) {
            if (ticket.getAutoTeleport()) continue; // We don't care about auto teleport tickets
            if (ticket.getPlayerIdsList().size() != 1) continue;

            UUID playerId = UUID.fromString(ticket.getPlayerIds(0));

            Pair<PlayerChooseInitialServerEvent, Continuation> pair = this.pendingPlayers.getIfPresent(playerId);
            if (pair == null) continue; // Likely submitted by a different service.

            this.pendingPlayers.invalidate(playerId);
            this.connectPlayerToAssignment(pair.left(), match.getAssignment());
            pair.right().resume();
        }
    }

    private void connectPlayerToAssignment(@NotNull PlayerChooseInitialServerEvent event, @NotNull Assignment assignment) {
        RegisteredServer registeredServer = this.proxy.getServer(assignment.getServerId()).orElseGet(() -> {
            InetSocketAddress address = new InetSocketAddress(assignment.getServerAddress(), assignment.getServerPort());
            return this.proxy.registerServer(new ServerInfo(assignment.getServerId(), address));
        });
        event.setInitialServer(registeredServer);
    }

    private void onEvict(@Nullable UUID playerId, @Nullable Pair<PlayerChooseInitialServerEvent, Continuation> pair, @NotNull RemovalCause cause) {
        if (cause != RemovalCause.EXPIRED) return;
        if (playerId == null || pair == null) return;

        pair.left().getPlayer().disconnect(ERROR_MESSAGE);
        pair.right().resume();
    }
}
