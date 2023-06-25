package dev.emortal.velocity.relationships.commands.friend;

import com.google.common.util.concurrent.Futures;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.grpc.mcplayer.McPlayerGrpc;
import dev.emortal.api.grpc.mcplayer.McPlayerProto;
import dev.emortal.api.grpc.mcplayer.PlayerTrackerGrpc;
import dev.emortal.api.liveconfigparser.configs.gamemode.GameModeCollection;
import dev.emortal.api.liveconfigparser.configs.gamemode.GameModeConfig;
import dev.emortal.api.model.mcplayer.McPlayer;
import dev.emortal.api.utils.ProtoTimestampConverter;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import dev.emortal.velocity.relationships.FriendCache;
import dev.emortal.velocity.utils.DurationFormatter;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

public class FriendListSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(FriendListSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final Component NO_FRIENDS_MESSAGE = MINI_MESSAGE.deserialize("<light_purple>You have no friends. Use /friend add <name> to add someone.");
    private static final String MESSAGE_TITLE = "<light_purple>----- Friends (Page <page>/<max_page>) -----</light_purple>";
    private static final String ONLINE_LINE = "<click:suggest_command:'/message <username> '><green><username> - <server></green></click>";
    private static final String OFFLINE_LINE = "<red><username> - Seen <last_seen></red>";
    private static final Component MESSAGE_FOOTER = Component.text("----------------------------", NamedTextColor.LIGHT_PURPLE);

    private final McPlayerGrpc.McPlayerFutureStub mcPlayerService;
    private final PlayerTrackerGrpc.PlayerTrackerFutureStub playerTrackerService;
    private final FriendCache friendCache;
    private final GameModeCollection gameModeCollection;

    public FriendListSub(McPlayerGrpc.McPlayerFutureStub mcPlayerService,
                         PlayerTrackerGrpc.PlayerTrackerFutureStub playerTrackerService, FriendCache friendCache,
                         GameModeCollection gameModeCollection) {
        this.mcPlayerService = mcPlayerService;
        this.playerTrackerService = playerTrackerService;
        this.friendCache = friendCache;
        this.gameModeCollection = gameModeCollection;
    }

    public int execute(CommandContext<CommandSource> context) {
        Player player = (Player) context.getSource();

        List<FriendCache.CachedFriend> friends = this.friendCache.get(player.getUniqueId());
        int maxPage = (int) Math.ceil(friends.size() / 8.0);

        if (maxPage == 0) {
            player.sendMessage(NO_FRIENDS_MESSAGE);
            return 1;
        }

        int page = context.getArguments().containsKey("page") ? Math.min(context.getArgument("page", Integer.class), maxPage) : 1;

        this.retrieveStatuses(friends, friendStatuses -> {
            Collections.sort(friendStatuses);
            List<FriendStatus> pageFriends = friendStatuses.stream()
                    .skip((page - 1) * 8L)
                    .limit(8).toList();

            player.sendMessage(this.createMessage(pageFriends, page, maxPage));
        });

        return 1;
    }

    private Component createMessage(List<FriendStatus> statuses, int page, int maxPage) {
        TextComponent.Builder message = Component.text()
                .append(
                        MINI_MESSAGE.deserialize(MESSAGE_TITLE,
                                Placeholder.parsed("page", String.valueOf(page)),
                                Placeholder.parsed("max_page", String.valueOf(maxPage)))
                ).append(Component.newline());

        for (FriendStatus status : statuses) {
            if (status.isOnline()) {
                message.append(
                        MINI_MESSAGE.deserialize(ONLINE_LINE,
                                Placeholder.parsed("username", status.getUsername()),
                                Placeholder.parsed("server", this.createActivityForServer(status.getServerId())))
                );
            } else {
                message.append(
                        MINI_MESSAGE.deserialize(OFFLINE_LINE,
                                Placeholder.parsed("username", status.getUsername()),
                                Placeholder.parsed("last_seen", DurationFormatter.formatShortFromInstant(status.getLastSeen())))
                );
            }
            message.append(Component.newline());
        }
        message.append(MESSAGE_FOOTER);
        return message.build();
    }

    private void retrieveStatuses(@NotNull List<FriendCache.CachedFriend> friends, Consumer<List<FriendStatus>> callback) {
        Map<UUID, FriendStatus> statuses = new ConcurrentHashMap<>();
        for (FriendCache.CachedFriend friend : friends)
            statuses.put(friend.playerId(), new FriendStatus(friend.playerId(), friend.friendsSince()));

        var playersRequest = this.mcPlayerService.getPlayers(McPlayerProto.GetPlayersRequest.newBuilder()
                .addAllPlayerIds(friends.stream().map(FriendCache.CachedFriend::playerId).map(UUID::toString).toList())
                .build());

        Futures.addCallback(playersRequest, FunctionalFutureCallback.create(
                response -> {
                    for (McPlayer player : response.getPlayersList()) {
                        FriendStatus status = statuses.get(UUID.fromString(player.getId()));
                        status.setUsername(player.getCurrentUsername());
                        status.setLastSeen(ProtoTimestampConverter.fromProto(player.getLastOnline()));
                        status.setOnline(player.hasCurrentServer());
                        status.setServerId(player.hasCurrentServer() ? player.getCurrentServer().getServerId() : null);
                    }

                    callback.accept(new ArrayList<>(statuses.values()));
                },
                error -> {
                    LOGGER.error("Failed to retrieve player statuses: ", error);
                    callback.accept(new ArrayList<>(statuses.values()));
                }
        ), ForkJoinPool.commonPool());
    }

    @Getter
    @Setter
    @ToString
    private static class FriendStatus implements Comparable<FriendStatus> {
        private final @NotNull UUID uuid;
        private final @NotNull Instant friendsSince;
        private String username;
        boolean online;
        private String serverId;
        private Instant lastSeen;

        private FriendStatus(@NotNull UUID uuid, @NotNull Instant friendsSince) {
            this.uuid = uuid;
            this.friendsSince = friendsSince;
        }

        public int compareTo(@NotNull FriendListSub.FriendStatus o) {
            if (this.online && !o.online) return -1;
            if (!this.online && o.online) return 1;
            if (!this.online) return o.lastSeen.compareTo(this.lastSeen); // both offline
            return this.username.compareTo(o.username);
        }
    }

    private String createActivityForServer(String serverId) {
        String[] parts = serverId.split("-");
        String[] serverTypeIdParts = Arrays.copyOf(parts, parts.length - 2);
        String fleetId = String.join("-", serverTypeIdParts);

        Optional<GameModeConfig> optionalGameMode = this.gameModeCollection.getAllConfigs().stream()
                .filter(config -> config.getFleetName().equals(fleetId))
                .findFirst();

        if (optionalGameMode.isPresent()) {
            GameModeConfig gameModeConfig = optionalGameMode.get();
            return gameModeConfig.getActivityNoun() + " " + gameModeConfig.getFriendlyName();
        }

        LOGGER.warn("Could not find friendly name for fleet {}", fleetId);
        return fleetId;
    }
}
