package dev.emortal.velocity.listener;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import dev.emortal.api.grpc.mcplayer.McPlayerGrpc;
import dev.emortal.api.kurushimi.Assignment;
import dev.emortal.api.kurushimi.CreateTicketRequest;
import dev.emortal.api.kurushimi.FrontendGrpc;
import dev.emortal.api.kurushimi.KurushimiStubCollection;
import dev.emortal.api.kurushimi.SearchFields;
import dev.emortal.api.kurushimi.Ticket;
import dev.emortal.api.kurushimi.WatchAssignmentRequest;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import dev.emortal.api.utils.callback.FunctionalStreamObserver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ForkJoinPool;

public class LobbySelectorListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(LobbySelectorListener.class);

    private static final Component ERROR_MESSAGE = MiniMessage.miniMessage().deserialize("<red>Failed to connect to lobby");

    private final McPlayerGrpc.McPlayerFutureStub mcPlayerService = GrpcStubCollection.getPlayerService().orElse(null);
    private final FrontendGrpc.FrontendFutureStub matchmakingService = KurushimiStubCollection.getFutureStub().orElse(null);
    private final FrontendGrpc.FrontendStub matchmakingServiceBlocking = KurushimiStubCollection.getStub().orElse(null);
    private final ProxyServer proxy;

    public LobbySelectorListener(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Subscribe
    public void onInitialServerChoose(PlayerChooseInitialServerEvent event, Continuation continuation) {
        this.sendToLobbyServer(event, continuation);
    }

    private void sendToLobbyServer(PlayerChooseInitialServerEvent event, Continuation continuation) {
        ListenableFuture<Ticket> listenableTicketFuture = this.matchmakingService.createTicket(CreateTicketRequest.newBuilder()
                .setTicket(
                        Ticket.newBuilder()
                                .setPlayerId(event.getPlayer().getUniqueId().toString())
                                .setSearchFields(
                                        SearchFields.newBuilder()
                                                .addTags("game.lobby")
                                )
                                .setNotifyProxy(false)
                ).build());

        Futures.addCallback(listenableTicketFuture, FunctionalFutureCallback.create(
                ticket -> {
                    String ticketId = ticket.getId();

                    // do nothing
                    this.matchmakingServiceBlocking.watchTicketAssignment(WatchAssignmentRequest.newBuilder().setTicketId(ticketId).build(),
                            FunctionalStreamObserver.create(
                                    response -> {
                                        Assignment assignment = response.getAssignment();
                                        this.connectPlayerToAssignment(event, assignment);
                                        continuation.resume();
                                    },
                                    throwable -> {
                                        event.getPlayer().disconnect(ERROR_MESSAGE);
                                        LOGGER.error("Failed to connect player to lobby", throwable);
                                        continuation.resumeWithException(throwable);
                                    },
                                    () -> {
                                    }
                            ));
                },
                throwable -> {
                    event.getPlayer().disconnect(ERROR_MESSAGE);
                    LOGGER.error("Failed to connect player to lobby", throwable);
                }
        ), ForkJoinPool.commonPool());
    }

    private void connectPlayerToAssignment(PlayerChooseInitialServerEvent event, Assignment assignment) {
        RegisteredServer registeredServer = this.proxy.getServer(assignment.getServerId()).orElseGet(() -> {
            InetSocketAddress address = new InetSocketAddress(assignment.getServerAddress(), assignment.getServerPort());
            return this.proxy.registerServer(new ServerInfo(assignment.getServerId(), address));
        });
        event.setInitialServer(registeredServer);
    }
}
